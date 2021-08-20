(ns ^:no-doc ^:private fierycod.holy-lambda.util
  (:require
   [fierycod.holy-lambda.retriever :as retriever]
   #?(:bb [cheshire.core :as json]
      :clj [jsonista.core :as json]))
  #?(:clj
     (:import
      [java.net URL HttpURLConnection]
      [java.io InputStream InputStreamReader])))

(defn println-err!
  [msg]
  (binding [*out* *err*]
    (println msg)))

(defn x->json-string
  [x]
  (if (string? x)
    x
    #?(:bb (json/generate-string x)
       :clj (json/write-value-as-string x)
       :default (throw (ex-info "Not implemented" {})))))

(defn x->json-bytes
  [x]
  #?(:bb (.getBytes (json/generate-string x))
     :clj (json/write-value-as-bytes x)
     :default (throw (ex-info "Not implemented" {}))))

(defn json-stream->x
  [^InputStream s]
  #?(:bb
     (json/parse-string (slurp (InputStreamReader. s "UTF-8")) true)
     :clj
     (json/read-value
      (slurp (InputStreamReader. s "UTF-8"))
      (json/object-mapper {:decode-key-fn true}))
     :default
     (throw (ex-info "Not implemented" {}))))

(defn json-string->x
  [^String s]
  #?(:bb
     (json/parse-string s true)
     :clj
     (json/read-value s (json/object-mapper {:decode-key-fn true}))
     :default
     (throw (ex-info "Not implemented" {}))))

(defn- normalize-headers
  [headers]
  (into {} (keep (fn [[k v]] (when k [(.toLowerCase (name k)) v]))) headers))

(defn response-event->normalized-event
  [event]
  (cond-> event
    (seq (:headers event))
    (update :headers normalize-headers)

    (seq (:multiValueHeaders event))
    (update :multiValueHeaders normalize-headers)))

(defn- content-type
  [x]
  (get (:headers x) "content-type"))

(defn- json-content-type?
  [ctype]
  (= ctype "application/json"))

(defn in->edn-event
  [^InputStream event-stream]
  (let [event (-> event-stream json-stream->x)
        ctype (content-type event)
        body (:body event)]
    (cond-> event
      (and (string? body)
           (json-content-type? ctype))
      (assoc :body (json-string->x body)))))

(defn response->bytes
  [?response]
  (let [response (retriever/<-wait-for-response ?response)
        bytes-response? #?(:clj (bytes? response)
                           :default false)
        ;; remove internals
        response (if-not bytes-response?
                   (dissoc response :fierycod.holy-lambda.interceptor/interceptors)
                   response)
        ctype (when-not bytes-response?
                (content-type response))]

    (cond
      bytes-response?
      response

      ;; Optimize the common case
      (json-content-type? ctype)
      (x->json-bytes (update response :body x->json-string))

      ;; Ack event
      (nil? response)
      (x->json-bytes {:body nil
                      :statusCode 200})

      :else 
      (x->json-bytes response))))

#?(:clj
   (def ^:private success-codes #{200 202 201}))

(defn success-code?
  [code]
  (success-codes code))

#?(:clj
   (defn- retrieve-body
     [^HttpURLConnection http-conn status]
     (if-not (success-codes status)
       (.getErrorStream http-conn)
       (.getInputStream http-conn))))

#?(:clj
   (defn http
     [method url-s & [response]]
     (let [push? (= method "POST")
           response-bytes (when push? (response->bytes (response-event->normalized-event response)))
           ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
           _ (doto ^HttpURLConnection http-conn
               (.setDoOutput push?)
               (.setRequestProperty "content-type" "application/json")
               (.setRequestMethod method))
           _ (when push?
               (let [output-stream (.getOutputStream http-conn)]
                 (if-not (bytes? response-bytes)
                   (do (println "[holy-lambda] Response has not beed parsed to bytes array:" response-bytes)
                       (throw (ex-info (str "[holy-lambda] Failed to parse response to byte array. Response type:" (type response-bytes)) {})))
                   (.write output-stream ^"[B" response-bytes))
                 (.flush output-stream)
                 (.close output-stream)))
           ;; It's not necessary to normalize the response headers for runtimes since
           ;; we only rely on Lambda-Runtime-Deadline-Ms and Lambda-Runtime-Aws-Request-Id headers
           headers (into {} (.getHeaderFields http-conn))
           status (.getResponseCode http-conn)]
       {:headers headers
        :status status
        :body (response-event->normalized-event (in->edn-event (retrieve-body http-conn status)))})))

(defn call
  ([afn-sym]
   (partial call afn-sym))
  ([afn-sym request]
   (afn-sym request)))

#?(:clj
   (defn envs
     []
     (into {} (System/getenv))))

(defn getf-header
  [headers prop]
  (cond-> (get headers prop)
    (seq (get headers prop)) first))

(defn ctx
  [envs* rem-time-fn fn-name fn-version fn-invoked-arn memory-limit
   aws-request-id log-group-name log-stream-name cognito-identity
   client-context]
  {:getRemainingTimeInMs rem-time-fn
   :fnName fn-name
   :fnVersion fn-version
   :fnInvokedArn fn-invoked-arn
   :memoryLimitInMb memory-limit
   :awsRequestId aws-request-id
   :logGroupName log-group-name
   :logStreamName log-stream-name
   :identity cognito-identity
   :clientContext client-context
   :envs envs*})

(defn exit!
  []
  (System/exit 1))
