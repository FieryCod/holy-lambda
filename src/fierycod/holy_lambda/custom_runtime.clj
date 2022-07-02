(ns fierycod.holy-lambda.custom-runtime
  (:require
   [clojure.edn :as edn]
   [fierycod.holy-lambda.util :as u]))

(defn- url
  [runtime iid path]
  (u/->str "http://" ^String runtime "/2018-06-01/runtime/invocation/" ^String iid ^String path))

(defn- ->aws-context
  [iid headers event]
  (let [request-context (event :requestContext)]
    {:getRemainingTimeInMs (fn []
                             (- (Long/parseLong (u/getf-header headers "Lambda-Runtime-Deadline-Ms"))
                                (System/currentTimeMillis)))
     :fnName               (System/getenv "AWS_LAMBDA_FUNCTION_NAME")
     :fnVersion            (System/getenv "AWS_LAMBDA_FUNCTION_VERSION")
     :fnInvokedArn         (u/->str "arn:aws:lambda:" ^String (System/getenv "AWS_REGION")
                                    ":" (or ^String (get request-context :accountId) "0000000")
                                    ":function:" ^String (System/getenv "AWS_LAMBDA_FUNCTION_NAME"))
     :memoryLimitInMb      (System/getenv "AWS_LAMBDA_FUNCTION_MEMORY_SIZE")
     :awsRequestId         iid
     :logGroupName         (System/getenv "AWS_LAMBDA_LOG_GROUP_NAME")
     :logStreamName        (System/getenv "AWS_LAMBDA_LOG_STREAM_NAME")
     :identity             (get request-context :identity)
     :clientContext        (get request-context :clientContext)}))

(defn- send-runtime-error
  [runtime iid ^Exception err disable-analytics?]
  (u/println-err! (u/->str "[holy-lambda] Runtime error:\n" (pr-str (Throwable->map err))))
  (let [response
        (u/http "POST" (url runtime iid "/error")
          {:errorMessage (.getMessage err)
           :errorType    (or
                           (:type (ex-data err))
                           (.getName (.getClass ^Class err)))
           :stackTrace   (mapv str (.getStackTrace err))}
          disable-analytics?)]
    (when-not (response :success?)
      (u/println-err! (u/->str "[holy-lambda] Runtime error sent failed.\n" (str (response :body))))
      (System/exit 1))))

(defn- send-response
  [runtime iid response disable-analytics?]
  (let [{:keys [success? body]} (u/http "POST" (url runtime iid "/response") response disable-analytics?)]
    (when-not success?
      (send-runtime-error runtime iid (u/->ex "AWS did not accept your lambda payload:\n" (pr-str body)) disable-analytics?))))

(defn- normalize-event
  [event]
  (let [body        (:body event)
        ctype       (u/content-type event)]

    (cond
      (u/json-content-type? ctype)
      (assoc event :body-parsed (u/json-string->x body))

      (u/edn-content-type? ctype)
      (assoc event :body-parsed (edn/read-string body))

      :else event)))

(defn next-iter
  [runtime handler-name routes disable-analitics?]
  (let [aws-event (u/http "GET" (url runtime "" "next") nil disable-analitics?)
        headers   (aws-event :headers)
        iid       (u/getf-header headers "Lambda-Runtime-Aws-Request-Id")
        handler   (routes handler-name)
        event     (aws-event :body)]

    ;; https://github.com/aws/aws-xray-sdk-java/blob/master/aws-xray-recorder-sdk-core/src/main/java/com/amazonaws/xray/contexts/LambdaSegmentContext.java#L40
    (when-let [trace-id (u/getf-header (aws-event :headers) "Lambda-Runtime-Trace-Id")]
      (System/setProperty "com.amazonaws.xray.traceHeader" trace-id))

    (when-not handler
      (send-runtime-error runtime iid (u/->ex "Handler " ^String handler-name " not found!") disable-analitics?)
      (System/exit 1))

    (when (aws-event :success?)
      (try
        (send-response runtime
                       iid
                       (handler {:event (normalize-event event)
                                 :ctx   (->aws-context iid headers event)})
                       disable-analitics?)
        (catch Exception err
          (send-runtime-error runtime iid err disable-analitics?))))))
