(ns ^:no-doc ^:private fierycod.holy-lambda.util
  (:require
   [fierycod.holy-lambda.retriever :as retriever]
   #?(:bb [cheshire.core :as json]
      :clj [jsonista.core :as json]))
  #?(:clj
     (:import
      [java.net URL HttpURLConnection]
      [java.io InputStream InputStreamReader])))

(def ^:private success-codes #{200 202 201})

#?(:clj
   (defn- retrieve-body
     [^HttpURLConnection http-conn status]
     (if-not (success-codes status)
       (.getErrorStream http-conn)
       (.getInputStream http-conn))))

(defn in->edn-event
  [^InputStream event]
  #?(:bb
     (json/parse-string (slurp (InputStreamReader. event "UTF-8")) true)
     :clj
     (json/read-value
      (slurp (InputStreamReader. event "UTF-8"))
      (json/object-mapper {:decode-key-fn true}))))

(defn success-code?
  [code]
  (success-codes code))

(defn response->bytes
  [?response]
  (let [response (retriever/<-wait-for-response ?response)
        ;; remove internals
        response (dissoc response :fierycod.holy-lambda.interceptor/interceptors)]
    (cond
      ;; Optimize the common case
      (= (get-in response [:headers "Content-Type"]) "application/json; charset=utf-8")
      #?(:bb
         (.getBytes ^String (json/generate-string (assoc response :body (json/generate-string (:body response)))))
         :clj
         (json/write-value-as-bytes (assoc response :body (json/write-value-as-string (:body response))))
         :cljs nil)

      ;; On text
      (= (get-in response [:headers "Content-Type"]) "text/plain; charset=utf-8")
      #?(:bb
         (.getBytes ^String (json/generate-string response))
         :clj
         (json/write-value-as-bytes response))

      ;; Ack event
      (nil? response)
      #?(:bb
         (.getBytes ^String (json/generate-string {:body ""
                                                   :statusCode 200
                                                   :headers {"Content-Type" "text/plain; charset=utf-8"}}))
         :clj
         (json/write-value-as-bytes {:body ""
                                     :statusCode 200
                                     :headers {"Content-Type" "text/plain; charset=utf-8"}})
         :cljs nil)

      ;; Handle redirect. Redirect should have nil? body
      (and (get-in response [:headers "Location"])
           (nil? (:body response)))
      #?(:bb
         (.getBytes ^String (json/generate-string response))
         :clj
         (json/write-value-as-bytes response)
         :cljs nil)

      ;; Corner cases should be handled via interceptor chain
      :else
      response)))

#?(:clj
   (defn http
     [method url-s & [response]]
     (let [push? (= method "POST")
           response-bytes (when push? (response->bytes response))
           ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
           _ (doto http-conn
               (.setDoOutput push?)
               (.setRequestProperty "Content-Type" "application/json")
               (.setRequestMethod method))
           _ (when push?
               (doto (.getOutputStream http-conn)
                 (.write ^"[B" response-bytes)
                 (.close)))
           headers (into {} (.getHeaderFields http-conn))
           status (.getResponseCode http-conn)]
       {:headers headers
        :status status
        :body (in->edn-event (retrieve-body http-conn status))})))

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
  ([headers]
   (partial getf-header headers))
  ([headers prop]
   (cond-> (get headers prop)
     (seq (get headers prop)) first)))

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

#?(:clj
   (defn exit!
     []
     (System/exit -1)))
