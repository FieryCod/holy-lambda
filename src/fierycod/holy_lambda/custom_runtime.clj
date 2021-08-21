(ns fierycod.holy-lambda.custom-runtime
  (:require
   [fierycod.holy-lambda.agent]
   [fierycod.holy-lambda.util :as u]))

(defn- ->ex
  [msg]
  (ex-info (str "[holy-lambda]: " msg) {}))

(defn- url
  [{:keys [runtime iid path]}]
  (str "http://" runtime "/2018-06-01/runtime/invocation/" iid path))

(defn- ->aws-context
  [headers event env-vars]
  (let [get-env (partial get env-vars)
        request-context (:requestContext event)]
    {:getRemainingTimeInMs  (fn []
                              (- (Long/parseLong (u/getf-header headers "Lambda-Runtime-Deadline-Ms"))
                                 (System/currentTimeMillis)))
     :fnName                (get-env "AWS_LAMBDA_FUNCTION_NAME")
     :fnVersion             (get-env "AWS_LAMBDA_FUNCTION_VERSION")
     :fnInvokedArn          (str "arn:aws:lambda:" (get-env "AWS_REGION")
                                 ":" (or (:accountId request-context) "0000000")
                                 ":function:" (get-env "AWS_LAMBDA_FUNCTION_NAME"))
     :memoryLimitInMb       (get-env "AWS_LAMBDA_FUNCTION_MEMORY_SIZE")
     :awsRequestId          (:requestId request-context)
     :logGroupName          (get-env "AWS_LAMBDA_LOG_GROUP_NAME")
     :logStreamName         (get-env "AWS_LAMBDA_LOG_STREAM_NAME")
     :identity              (:identity request-context)
     :clientContext         (:clientContext request-context)
     :envs                  env-vars}))

(defn- send-runtime-error
  [runtime iid ^Exception err]
  (u/println-err! (str "[holy-lambda] Runtime error:\n" err))
  (let [response (u/http "POST" (url {:runtime runtime
                                      :iid iid
                                      :path "/error"})
                         {:statusCode 500
                          :headers {"content-type" "application/json"}
                          :body {:runtime-error true
                                 :err (Throwable->map err)}})]
    (when-not (u/success-code? (:status response))
      (u/println-err! (str "[holy-lambda] Runtime error failed sent to AWS.\n" (:body response)))
      (System/exit 1))))

(defn- fetch-aws-event
  [runtime]
  (let [aws-event (u/http "GET" (url {:runtime runtime
                                      :path "next"}))]
    (assoc aws-event :invocation-id (u/getf-header (:headers aws-event) "Lambda-Runtime-Aws-Request-Id"))))

(defn- send-response
  [runtime iid response]
  (let [{:keys [status body]} (u/http "POST" (url {:runtime runtime
                                                   :iid iid
                                                   :path "/response"})
                                      response)]

    (when-not (u/success-code? status)
      (send-runtime-error runtime iid (->ex (str "AWS did not accept your lambda payload:\n" body))))))

(defn- process-event
  [runtime iid aws-event env-vars handler]
  (let [event (-> aws-event :body)
        context (->aws-context (:headers aws-event) event env-vars)]
    (try
      (send-response runtime iid (handler {:event event
                                           :ctx context}))
      (catch Exception err
        (send-runtime-error runtime iid err)))))

(defn next-iter
  [maybe-handler routes env-vars]
  (let [runtime (get env-vars "AWS_LAMBDA_RUNTIME_API")
        handler-name (or maybe-handler (get env-vars "_HANDLER"))
        aws-event (fetch-aws-event runtime)
        handler (get routes handler-name)
        iid (:invocation-id aws-event)]

    ;; https://github.com/aws/aws-xray-sdk-java/blob/master/aws-xray-recorder-sdk-core/src/main/java/com/amazonaws/xray/contexts/LambdaSegmentContext.java#L40
    (when-let [trace-id (u/getf-header (:headers aws-event) "Lambda-Runtime-Trace-Id")]
      (System/setProperty "com.amazonaws.xray.traceHeader" trace-id))

    (when-not handler
      (send-runtime-error runtime iid (->ex (str "Handler " handler-name " not found!")))
      (System/exit 1))

    (when-not iid
      (send-runtime-error runtime iid (->ex (str "Failed to determine new invocation-id. Invocation id is:" iid)))
      (System/exit 1))

    (when (and (:invocation-id aws-event) (u/success-code? (:status aws-event)))
      (process-event runtime iid aws-event env-vars handler))))
