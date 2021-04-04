(ns ^:no-doc ^:private fierycod.holy-lambda.native-runtime
  (:require
   [fierycod.holy-lambda.util :as u]))

(defn ->ex
  [msg]
  (Exception. (str "[Holy Lambda]: " msg)))

(defn url
  [{:keys [runtime iid path]}]
  (str "http://" runtime "/2018-06-01/runtime/invocation/" iid path))

(defn- ->aws-context
  [headers event env-vars]
  (let [get-env (partial get env-vars)
        getf-header (u/getf-header headers)
        request-context (:requestContext event)]
    (u/ctx env-vars
           (fn []
             (- (Long/parseLong (getf-header "Lambda-Runtime-Deadline-Ms"))
                (System/currentTimeMillis)))
           (get-env "AWS_LAMBDA_FUNCTION_NAME")
           (get-env "AWS_LAMBDA_FUNCTION_VERSION")
           (str "arn:aws:lambda:" (get-env "AWS_REGION")
                ":" (or (:accountId request-context) "0000000")
                ":function:" (get-env "AWS_LAMBDA_FUNCTION_NAME"))
           (get-env "AWS_LAMBDA_FUNCTION_MEMORY_SIZE")
           (:requestId request-context)
           (get-env "AWS_LAMBDA_LOG_GROUP_NAME")
           (get-env "AWS_LAMBDA_LOG_STREAM_NAME")
           (:identity request-context)
           (:clientContext request-context))))

(defn- send-runtime-error
  [runtime iid ^Exception err]
  (let [message (.getMessage err)
        response (u/http "POST" (url {:runtime runtime
                                      :iid iid
                                      :path "/error"})
                         {:errorMessage message
                          :errorType (-> err (.getClass) (.getCanonicalName))})]
    (println "[Holy Lambda] Runtime error:" message)
    (when-not (u/success-code? (:status response))
      (println "[Holy Lambda] Runtime error failed sent to AWS." (:body response))
      (u/exit!))))

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
  [routes env-vars]
  (let [runtime (get env-vars "AWS_LAMBDA_RUNTIME_API")
        handler-name (get env-vars "_HANDLER")
        aws-event (fetch-aws-event runtime)
        handler (get routes handler-name)
        iid (:invocation-id aws-event)]

    (when-not handler
      (send-runtime-error runtime iid (->ex (str "Handler " handler-name " not found!")))
      (u/exit!))

    (when-not iid
      (send-runtime-error runtime iid (->ex (str "Failed to determine new invocation-id. Invocation id is:" iid)))
      (u/exit!))

    (when (and (:invocation-id aws-event) (u/success-code? (:status aws-event)))
      (process-event runtime iid aws-event env-vars (u/call handler)))))
