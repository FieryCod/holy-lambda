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

;; TODO: Tidy up event read/write
;; TODO: Local SAM environment passes headers as is without lowercasing
;; API gateway instead makes all the headers lowercase. We should probably process all the headers and lowercase them to make the environments consistent
(defn in->edn-event
  [^InputStream event]
  (let [event #?(:bb
                 (json/parse-string (slurp (InputStreamReader. event "UTF-8")) true)
                 :clj
                 (json/read-value
                  (slurp (InputStreamReader. event "UTF-8"))
                  (json/object-mapper {:decode-key-fn true}))
                 :default
                 nil)
        content-type (or (:Content-Type (:headers event))
                         (:content-type (:headers event)))
        body (:body event)]

    (if (and (not= content-type "application/json")
             (not= content-type "application/json; charset=utf-8"))
      event
      (assoc event
             :body
             (if-not (string? body)
               body
               #?(:bb
                  (json/parse-string body true)
                  :clj
                  (json/read-value body (json/object-mapper {:decode-key-fn true}))
                  :default
                  nil))))))

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

      (contains? #{"text/plain"
                   "text/plain; charset=utf-8"
                   "text/html"
                   "text/html; charset=utf-8"}
                 (get-in response [:headers "Content-Type"]))
      #?(:bb
         (.getBytes ^String (json/generate-string response))
         :clj
         (json/write-value-as-bytes response)
         :cljs nil)

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
               (let [output-stream (.getOutputStream http-conn)]
                 (if-not (bytes? response-bytes)
                   (do (println "[holy-lambda] Response has not beed parsed to bytes array:" response-bytes)
                       (throw (ex-info (str "[holy-lambda] Failed to parse response to byte array. Response type:" (type response-bytes)) {})))
                   (.write output-stream ^"[B" response-bytes))
                 (.flush output-stream)
                 (.close output-stream)))
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
