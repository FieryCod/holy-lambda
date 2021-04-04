(ns ^:no-doc ^:private fierycod.holy-lambda.util
  (:require
   [jsonista.core :as json])
  (:import
   [java.net URL HttpURLConnection]
   [java.io InputStream InputStreamReader]))

(def ^:private success-codes #{200 202 201})

(defn- retrieve-body
  [^HttpURLConnection http-conn status]
  (if-not (success-codes status)
    (.getErrorStream http-conn)
    (.getInputStream http-conn)))

(defn in->edn-event
  [^InputStream event]
  (json/read-value
   (slurp (InputStreamReader. event "UTF-8"))
   (json/object-mapper {:decode-key-fn true})))

(defn success-code?
  [code]
  (success-codes code))

(defn ->payload-bytes
  [payload]
  (cond
    (or (nil? payload)
        (string? payload))
    (json/write-value-as-bytes {:body payload
                                :statusCode 200
                                :headers {"Content-Type" "text/plain"}})

    (= (get-in payload [:headers "Content-Type"]) "application/json")
    (json/write-value-as-bytes (assoc payload :body (json/write-value-as-string (:body payload))))

    ;; Corner cases should be handled via interceptor chain
    :else
    payload))

(defn http
  [method url-s & [payload]]
  (let [push? (= method "POST")
       payload-bytes (when push? (->payload-bytes payload))
       ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
        _ (doto http-conn
            (.setDoOutput push?)
            (.setRequestProperty "Content-Type" "application/json")
            (.setRequestMethod method))
        _ (when push?
            (doto (.getOutputStream http-conn)
              (.write (bytes payload-bytes))
              (.close)))
        headers (into {} (.getHeaderFields http-conn))
        status (.getResponseCode http-conn)]
    {:headers headers
     :status status
     :body (in->edn-event (retrieve-body http-conn status))}))

(defn call
  ([afn-sym]
   (partial call afn-sym))
  ([afn-sym request]
   (afn-sym request)))

(defn envs
  []
  (into {} (System/getenv)))

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

(defn exit!
  []
  (System/exit -1))
