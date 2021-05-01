(ns fierycod.holy-lambda.native-runtime
  (:require
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.agent]
   [fierycod.holy-lambda.util :as u]))

(defn- ->ex
  [msg]
  (throw (ex-info (str "[holy-lambda]: " msg) {})))

(defn- url
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
  (println "[holy-lambda] Runtime error:" err)
  (let [response (u/http "POST" (url {:runtime runtime
                                      :iid iid
                                      :path "/error"})
                         (hr/json {:runtime-error true
                                   :err (Throwable->map err)}))]
    (when-not (u/success-code? (:status response))
      (println "[holy-lambda] Runtime error failed sent to AWS." (:body response))
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

(defn- next-iter
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

(defmacro entrypoint
  "Generates the entrypoint function which has the two roles:
  1. The `-main` might be then launched by AWS in the lambda runtime.
     Lambda runtime tries to proxy the payloads from AWS to corresponding handlers
     defined in `native-template.yml`.

  2. The `-main` might be used to generate the configuration necessary to compile
     the project to native.

     *For more info take a look into the corresponding links:*
     1. https://github.com/oracle/graal/issues/1367
     2. https://github.com/oracle/graal/blob/master/substratevm/CONFIGURE.md
     3. https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md

     According to the comment of the @cstancu with the help of the agent we can find the majority
     of the reflective calls and generate the configuration. Generated configuration might then be used
     by `native-image` tool.

  Usage:
  ```
   (entrypoint [#'ExampleLambda1 #'ExampleLambda2 #'ExampleLambda3])
  ```"
  {:added "0.0.1"}
  [lambdas]
  `(do
     (def ~'PRVL_ROUTES (into {} (mapv (fn [l#] [(str (str (:ns (meta l#)) "." (str (:name (meta l#))))) l#]) ~lambdas)))
     (defn ~'-main []
       ;; executor = native-agent    -- Indicates that the configuration for compiling via `native-image`
       ;;                               will be generated via the agent.
       ;;                               Example in: `examples/sqs-example/Makefile` at `gen-native-configuration` command
       ;;
       ;; executor = anything else   -- Run provided runtime loop
       (if (= (System/getProperty "executor") @#'fierycod.holy-lambda.agent/AGENT_EXECUTOR)
         ;; Generate the native configuration for the lambdas
         (#'fierycod.holy-lambda.agent/routes->reflective-call! ~'PRVL_ROUTES)

         ;; Start native runtime loop
         (while true
           (#'fierycod.holy-lambda.native-runtime/next-iter ~'PRVL_ROUTES (#'fierycod.holy-lambda.util/envs)))))))
