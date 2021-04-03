(ns fierycod.holy-lambda.core
  "This namespace integrates the Clojure code with two different runtimes: Java Lambda Runtime, Native Provided Runtime.
  The former is the Official Java Runtime for AWS Lambda which is well tested and works perfectly fine, but it's rather slow due to cold starts.
  The latter is a custom [runtime](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html) integrated within the framework.
  It's a significantly faster than the Java runtime due to the use of GraalVM.

  *Namespace includes:*
  - Friendly macro for generating Lambda functions which run on both runtimes
  - TODO Utilities which help produce valid response"
  (:require
   [fierycod.holy-lambda.impl.agent]
   [fierycod.holy-lambda.impl.util :as u]
   [clojure.data.json :as json]
   [clojure.walk :as w]
   [clojure.tools.macro :as macro])
  (:import
   [java.io InputStream OutputStream InputStreamReader]
   [com.amazonaws.services.lambda.runtime RequestStreamHandler Context CognitoIdentity ClientContext Client]))

(def ^:dynamic ^:private *runtime* nil)
(def ^:dynamic ^:private *invocation-id* nil)

(def ^{:added "0.0.1"
       :arglists '([afn-sym]
                   [afn-sym request])}
  call
  "Resolves the lambda function and calls it with the event and context.
  Returns the callable lambda function if only one argument is passed.
  See `fierycod.holy-lambda.impl.util/call`"
  #'fierycod.holy-lambda.impl.util/call)

(defn- gen-class-lambda
  [prefix gfullname]
  `(gen-class
    :name ~gfullname
    :prefix ~prefix
    :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn- getf-header*
  ([headers]
   (partial getf-header* headers))
  ([headers prop]
   (cond-> (get headers prop)
     (seq (get headers prop)) first)))

(defn- keywordize-hashmap
  [m]
  (w/keywordize-keys (into {} m)))

(defn- ctx
  [envs rem-time fn-name fn-version fn-invoked-arn memory-limit
   aws-request-id log-group-name log-stream-name
   identity client-context]
  {:remainingTimeInMs rem-time
   :fnName fn-name
   :fnVersion fn-version
   :fnInvokedArn fn-invoked-arn
   :memoryLimitInMb memory-limit
   :aws-request-id aws-request-id
   :logGroupName log-group-name
   :logStreamName log-stream-name
   :identity identity
   :clientContext client-context
   :envs envs})

(defn- ctx-object->ctx-edn
  [^Context context envs]
  (ctx envs (.getRemainingTimeInMillis context) (.getFunctionName context) (.getFunctionVersion context) (.getInvokedFunctionArn context)
       (.getMemoryLimitInMB context) (.getAwsRequestId context) (.getLogGroupName context) (.getLogStreamName context)
       (when-let [^CognitoIdentity identity (.getIdentity context)]
         {:identityId (.getIdentityId identity)
          :identityPoolId (.getIdentityPoolId identity)})
       (when-let [^ClientContext client-context (.getClientContext context)]
         {:client (when-let [^Client client (.getClient client-context)]
                    {:installationId (.getInstallationId client)
                     :appTitle (.getAppTitle client)
                     :appVersionName (.getAppVersionName client)
                     :appVersionCode (.getAppVersionCode client)
                     :appPackageName (.getAppPackageName client)})
          :custom (keywordize-hashmap (.getCustom client-context))
          :environment (keywordize-hashmap (.getEnvironment client-context))})))

(defn- define-synthetic-name
  [aname sym]
  `(def ~(with-meta aname (meta aname))
     ~sym))

(defn- envs
  []
  (keywordize-hashmap (System/getenv)))

(defn- wrap-lambda
  [gmethod-sym fn-args fn-body gclass]
  (let [lambda `(fn ~fn-args ~@fn-body)]
    `(do
       ~gclass
       (defn ~gmethod-sym
         ;; Arity used for testing and native runtime invocation
         ([request#]
          (~lambda request#))
           ;; Arity used for Java runtime
         ([this# ^InputStream in# ^OutputStream out# ^Context ctx#]
          (let [event# (#'fierycod.holy-lambda.impl.util/in->edn-event in#)
                context# (#'fierycod.holy-lambda.core/ctx-object->ctx-edn ctx# (#'fierycod.holy-lambda.core/envs))
                response# (~lambda {:event event#
                                    :ctx context#})
                f-response# (assoc response# :body (json/write-str (:body response#)))]
            (.write out# (.getBytes ^String (json/write-str f-response#) "UTF-8"))))))))

(defn- native->aws-context
  [headers event env-vars]
  (let [get-env (partial get env-vars)
        getf-header (getf-header* headers)]
    (ctx env-vars
         (- (Long/parseLong (getf-header "Lambda-Runtime-Deadline-Ms")) (System/currentTimeMillis))
         (get-env :AWS_LAMBDA_FUNCTION_NAME)
         (get-env :AWS_LAMBDA_FUNCTION_VERSION)
         (str "arn:aws:lambda:" (get-env :AWS_REGION)
              ":" (get-in event [:requestContext :accountId] "0000000")
              ":function:" (get-env :AWS_LAMBDA_FUNCTION_NAME))
         (get-env :AWS_LAMBDA_FUNCTION_MEMORY_SIZE)
         (-> event :requestContext :requestId)
         (get-env :AWS_LAMBDA_LOG_GROUP_NAME)
         (get-env :AWS_LAMBDA_LOG_STREAM_NAME)
         ;; #8
         {:identityId "????"
          :identityPoolId (-> event :requestContext :identity :cognitoIdentityPoolId)}
         ;; #7
         {:client nil :custom nil :environment nil})))

(defn- send-runtime-error
  [^Exception err]
  (let [exit! #(System/exit -1)
        url (str "http://" *runtime* "/2018-06-01/runtime/invocation/" *invocation-id* "/error")
        message (.getMessage err)
        payload {:errorMessage message
                 :errorType (-> err (.getClass) (.getCanonicalName))}
        response (u/http "POST" url payload)]
    (println "[Holy Lambda] Runtime error" message)
    (when-not (u/success-code? (:status response))
      (do (println "[Holy Lambda] Response of handler failed to be sent to AWS. " (:body response))
          (exit!)))))

(defn- fetch-aws-event
  [runtime]
  (let [url (str "http://" runtime "/2018-06-01/runtime/invocation/next")
        aws-event (u/http "GET" url)]
    (assoc aws-event :invocation-id (getf-header* (:headers aws-event) "Lambda-Runtime-Aws-Request-Id"))))

(defn- send-response
  [response]
  (let [url (str "http://" *runtime* "/2018-06-01/runtime/invocation/" *invocation-id* "/response")
        {:keys [status body]} (u/http "POST" url response)]
    (when-not (u/success-code? status)
      (send-runtime-error (Exception. ^String (str "AWS did not accept the your lambda payload:\n" body))))))

(defn- process-event
  [aws-event env-vars handler]
  (let [event (-> aws-event :body)
        context (native->aws-context (:headers aws-event) event env-vars)]
    (try
      (send-response (handler {:event event
                               :ctx context}))
      (catch Exception err
        (send-runtime-error err)))))

(defn- next-iter
  [routes env-vars]
  (let [runtime* (:AWS_LAMBDA_RUNTIME_API env-vars )
        handler-name (:_HANDLER env-vars)
        aws-event (fetch-aws-event runtime*)
        invocation-id* (:invocation-id aws-event)
        handler (get routes handler-name)]
    (binding [*runtime* runtime*
              *invocation-id* invocation-id*]
      (if-not handler
        (send-runtime-error (Exception. ^String (str "Handler: " handler-name " not found!")))
        (when (and *invocation-id* (u/success-code? (:status aws-event)))
          (process-event aws-event env-vars (u/call handler)))))))

(defmacro deflambda
  "Similiar to `defn`, with the limitation that it only allows the
  2 arity definition. Defined Lambda is safe to use either in Java Runtime or
  with the Custom Runtime alongside this library."
  {:arglists '([name doc-string? attrs-map? [event context] prepost-map? fn-body]
               [name doc-string? attrs-map? [input output context] prepost-map? fn-body])
   :added "0.0.1"}
  [name & attrs]
  (let [[aname [fn-args & fn-body]] (macro/name-with-attributes name attrs)
        aname (vary-meta aname assoc :arity (count fn-args))
        prefix (str "PRVL" aname "--")
        gmethod-sym (symbol (str prefix "handleRequest"))
        gfullname (symbol (str (ns-name *ns*) "." aname))
        gclass (gen-class-lambda prefix gfullname)]

    `(do ~(wrap-lambda gmethod-sym fn-args fn-body gclass)
         ~(define-synthetic-name aname gmethod-sym))))

(defmacro gen-main
  "Generates the main function which has the two roles:
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
     by `native-image` tool."
  {:added "0.0.1"}
  [lambdas]
  `(defn ~'-main []
     (let [fnames# (map (comp #(str (str (:ns %) "." (str (:name %)))) meta) ~lambdas)
           routes# (into {} (mapv vector fnames# ~lambdas))
           executor# (System/getProperty "executor")]

       ;; executor = native-agent    -- Indicates that the configuration for compiling via `native-image` will be generated via the agent
       ;;                               Example in: `examples/sqs-example/Makefile` at `gen-native-configuration` command
       ;; executor = anything else   -- Run provided runtime loop
       (if (= executor# "native-agent")
         ;; When we want to generate the native configuration for the lambdas
         (#'fierycod.holy-lambda.impl.agent/routes->reflective-call! routes#)

         ;; Otherwise just start the runtime loop
         (while true
           (#'fierycod.holy-lambda.core/next-iter routes# (#'fierycod.holy-lambda.core/envs)))))))
