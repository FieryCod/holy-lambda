(ns fierycod.holy-lambda.custom-runtime
  (:require
   [fierycod.holy-lambda.agent]
   [fierycod.holy-lambda.util :as u]))

(defn- url
  [runtime iid path]
  (u/->str "http://" runtime "/2018-06-01/runtime/invocation/" iid path))

(defn- ->aws-context
  [iid headers event env-vars]
  (let [request-context (event :requestContext)]
    {:getRemainingTimeInMs  (fn []
                              (- (Long/parseLong (u/getf-header headers "Lambda-Runtime-Deadline-Ms"))
                                 (System/currentTimeMillis)))
     :fnName                (env-vars "AWS_LAMBDA_FUNCTION_NAME")
     :fnVersion             (env-vars "AWS_LAMBDA_FUNCTION_VERSION")
     :fnInvokedArn          (u/->str "arn:aws:lambda:" (env-vars "AWS_REGION")
                                     ":" (or (request-context :accountId) "0000000")
                                     ":function:" (env-vars "AWS_LAMBDA_FUNCTION_NAME"))
     :memoryLimitInMb       (env-vars "AWS_LAMBDA_FUNCTION_MEMORY_SIZE")
     :awsRequestId          iid
     :logGroupName          (env-vars "AWS_LAMBDA_LOG_GROUP_NAME")
     :logStreamName         (env-vars "AWS_LAMBDA_LOG_STREAM_NAME")
     :identity              (request-context :identity)
     :clientContext         (request-context :clientContext)
     :envs                  env-vars}))

(defn- send-runtime-error
  [runtime iid ^Exception err]
  (u/println-err! (u/->str "[holy-lambda] Runtime error:\n" err))
  (let [response (u/http "POST" (url runtime iid "/error")
                         {:statusCode 500
                          :headers {"content-type" "application/json"}
                          :body {:runtime-error true
                                 :err (Throwable->map err)}})]
    (when-not (response :success?)
      (u/println-err! (u/->str "[holy-lambda] Runtime error failed sent to AWS.\n" (response :body)))
      (System/exit 1))))

(defn- send-response
  [runtime iid response]
  (let [{:keys [status body]} (u/http "POST" (url runtime iid "/response") response)]
    (when-not (status :success?)
      (send-runtime-error runtime iid (u/->ex "AWS did not accept your lambda payload:\n" body)))))

(defn next-iter
  [maybe-handler routes env-vars]
  (let [runtime (env-vars "AWS_LAMBDA_RUNTIME_API")
        handler-name (or maybe-handler (env-vars "_HANDLER"))
        aws-event (u/http "GET" (url runtime nil "next"))
        headers (aws-event :headers)
        iid (u/getf-header headers "Lambda-Runtime-Aws-Request-Id")
        handler (routes handler-name)
        event (aws-event :body)]

    ;; https://github.com/aws/aws-xray-sdk-java/blob/master/aws-xray-recorder-sdk-core/src/main/java/com/amazonaws/xray/contexts/LambdaSegmentContext.java#L40
    (when-let [trace-id (u/getf-header (aws-event :headers) "Lambda-Runtime-Trace-Id")]
      (System/setProperty "com.amazonaws.xray.traceHeader" trace-id))

    (when-not handler
      (send-runtime-error runtime iid (u/->ex "Handler " handler-name " not found!"))
      (System/exit 1))

    (when-not iid
      (send-runtime-error runtime iid (u/->ex "Failed to determine new invocation-id:" iid))
      (System/exit 1))

    (when (and iid (aws-event :success?))
      (try
        (send-response runtime iid (handler {:event event :ctx (->aws-context iid headers event env-vars)}))
        (catch Exception err
          (send-runtime-error runtime iid err))))))
