(ns fierycod.holy-lambda.core
  ^{:doc ""
    :author "Karol Wójcik"}
  (:require
   [clojure.data.json :as json]
   [clojure.tools.macro :as macro])
  (:import
   [java.time Instant]
   [java.util Date]
   [java.net URL HttpURLConnection]
   [java.io InputStream OutputStream InputStreamReader]
   [org.apache.logging.log4j LogManager]
   [com.amazonaws.services.lambda.runtime RequestStreamHandler Context CognitoIdentity ClientContext Client]))

(def ^:dynamic runtime nil)
(def ^:dynamic request-id nil)

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

(defn- ctx
  [rem-time fn-name fn-version fn-invoked-arn memory-limit
   aws-request-id log-group-name log-stream-name
   identity client-context logger]
  {:remainingTimeInMs rem-time
   :fnName fn-name
   :fnVersion fn-version
   :fnInvokedArn fn-invoked-arn
   :memoryLimitInMb memory-limit
   :awsRequestId aws-request-id
   :logGroupName log-group-name
   :logStreamName log-stream-name
   :identity identity
   :clientContext client-context
   :logger logger})

(defn- ctx-object->ctx-edn
  [^Context context]
  (ctx (.getRemainingTimeInMillis context) (.getFunctionName context) (.getFunctionVersion context) (.getInvokedFunctionArn context)
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
          :custom (into {} (.getCustom client-context))
          :environment (into {} (.getEnvironment client-context))})
       (.getLogger context)))

(defn- in->edn-event
  [^InputStream event]
  (json/read (InputStreamReader. event "utf-8") :key-fn keyword))

(defn- define-synthetic-name
  [aname sym]
  `(def ~(with-meta aname (meta aname))
     ~sym))

(defn- wrap-lambda
  [gmethod-sym fn-args fn-body gclass]
  (let [lambda `(fn ~fn-args ~@fn-body)]
    (condp = (count fn-args)
      2
      `(do
         ~gclass
         (defn ~gmethod-sym
           ;; This arity is used for testing and native runtime invocation
           ([event# context#]
            (~lambda event# context#))
           ([this# ^InputStream in# ^OutputStream out# ^Context ctx#]
            (let [event# (#'fierycod.holy-lambda.core/in->edn-event in#)
                  context# (#'fierycod.holy-lambda.core/ctx-object->ctx-edn ctx#)
                  aws-event# (-> (~lambda event# context#) (json/write-str))]
              (.write out# (.getBytes ^String aws-event# "UTF-8"))))))
      3
      ;; TODO: Validate whether lambada style would be helpful

      ;; If yes then we need to provide following code to support it on native side:
      ;; EVENT:
      ;; 1. Parse string to edn
      ;; 2. Parse edn to string
      ;; 3. Change string to InputStream
      ;; CONTEXT:
      ;; The same thing we should do with context
      ;; `(do
      ;;    ~gclass
      ;;    (defn ~gmethod-sym
      ;;      ([this# ^InputStream in# ^OutputStream out# ^Context ctx#]
      ;;       (~lambda in# out# ctx#))))
      (throw (Exception. "Lambada style is not supported for now. Check source code!"))

      ;; default
      (throw (Exception. "Invalid arity. Skipping wrap!"))

      )
    ))

(defn- native->aws-context
  [headers event env-vars]
  (let [get-env (partial get env-vars)
        getf-header (getf-header* headers)]
    (ctx (- (Long/parseLong (getf-header "Lambda-Runtime-Deadline-Ms")) (System/currentTimeMillis))
         (get-env "AWS_LAMBDA_FUNCTION_NAME")
         (get-env "AWS_LAMBDA_FUNCTION_VERSION")
         (str "arn:aws:lambda:" (get-env "AWS_REGION")
              ":" (get-in event [:requestContext :accountId] "0000000")
              ":function:" (get-env "AWS_LAMBDA_FUNCTION_NAME"))
         (get-env "AWS_LAMBDA_FUNCTION_MEMORY_SIZE")
         (-> event :requestContext :requestId)
         (get-env "AWS_LAMBDA_LOG_GROUP_NAME")
         (get-env "AWS_LAMBDA_LOG_STREAM_NAME")
         ;; #8
         {:identityId "????"
          :identityPoolId (-> event :requestContext :identity :cognitoIdentityPoolId)}
         ;; #7
         {:client nil :custom nil :environment nil}
         ;; #6
         nil
         ))
  )

(def ^:private success-codes #{200 202})

(defn- retrieve-body
  [^HttpURLConnection http-conn status]
  (if-not (success-codes status)
    (.getErrorStream http-conn)
    (.getInputStream http-conn)))

(defn- http-get
  "Internal http GET method used to communicate with lambda API"
  [url-s]
  (let [^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
        _ (doto http-conn
            (.setDoOutput false)
            (.setRequestMethod  "GET"))
        headers (into {} (.getHeaderFields http-conn))
        status (.getResponseCode http-conn)
        aws-event {:headers headers
                   :status status
                   :body (in->edn-event (retrieve-body http-conn status))}]
    aws-event))

(defn- http-post
  "Internal http POST method used to communicate with lambda API"
  [url-s body]
  (let [^String body-s (if (string? body) body (json/write-str body))
        ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
        _ (doto http-conn
            (.setDoOutput true)
            (.setRequestMethod  "POST"))
        _ (doto (.getOutputStream http-conn)
            (.write (.getBytes body-s "UTF-8"))
            (.flush)
            (.close))
        headers (into {} (.getHeaderFields http-conn))
        status (.getResponseCode http-conn)
        aws-event {:headers headers
                   :status status
                   :body (in->edn-event (retrieve-body http-conn status))}]
    aws-event))

(defn- send-runtime-error
  [^Exception err]
  (let [exit! #(System/exit -1)
        {:keys [status]} (http-post (str "http://" runtime "/2018-06-01/runtime/invocation/" request-id "/error")
                                    {:errorMessage (.getMessage err)
                                     :errorType (-> err (.getClass) (.getCanonicalName))})]
    (if (success-codes status)
      (exit!)
      (do
        ;; #5 Log error via custom logger
        (exit!)))))

(defn- fetch-aws-event
  [runtime]
  (let [aws-event (http-get (str "http://" runtime "/2018-06-01/runtime/invocation/next"))]
    (assoc aws-event :request-id (getf-header* (:headers aws-event) "Lambda-Runtime-Aws-Request-Id"))))

(defn call
  "Resolves the lambda function using provided ns and calls it with the event and context"
  {:added "0.0.1"
   :arglists '([afn-sym]
               [afn-sym event context]
               [afn-sym input output context])}
  ([afn-sym]
   (partial call afn-sym))
  ([afn-sym & args]
   (let [{:keys [arity ns name]} (meta afn-sym)]
     (assert (= arity (count args))
             (str "Function defined with two arguments should call lambda with only two arguments. "
                  "Otherwise use Lambada style and call with three arguments.\n\n"
                  "Failed when calling: '" ns "." name "\n"))
     (apply afn-sym args))))

(defn- send-response
  [response]
  (println (str "http://" runtime "/2018-06-01/runtime/invocation/" request-id "/response"))
  (let [{:keys [status body]} (http-post (str "http://" runtime "/2018-06-01/runtime/invocation/" request-id "/response")
                                                 (json/write-str response))]
    (when-not (success-codes status)
      (send-runtime-error (Exception. (str "AWS did not accept the your lambda payload:\n" body))))))

(defn- process-event
  [aws-event env-vars handler]
  (let [event (-> aws-event :body)
        context (native->aws-context (:headers aws-event) event env-vars)]
    (try
      (send-response (handler event context))
      (catch Exception err
        (send-runtime-error err)))))

(defn- next-iter
  [routes env-vars]
  (let [runtime* (get env-vars "AWS_LAMBDA_RUNTIME_API")
        handler-name (get env-vars "_HANDLER")
        aws-event (fetch-aws-event runtime*)
        request-id* (:request-id aws-event)
        handler (get routes handler-name)]
    (binding [runtime runtime*
              request-id request-id*]
      (println handler request-id* runtime)
      (if-not handler
        (send-runtime-error (Exception. (str "Handler: " handler-name " not found!")))
        (if (and request-id (success-codes (:status aws-event)))
          (process-event aws-event env-vars (call handler))
          ;; Log the error #10
          nil)))))

(defmacro deflambda
  ""
  {:arglists '([name doc-string? attrs-map? [event context] prepost-map? fn-body]
               [name doc-string? attrs-map? [input output context] prepost-map? fn-body])
   :added "0.0.1"}

  [name & attrs]
  (let [[aname [fn-args & fn-body]] (macro/name-with-attributes name attrs)
        aname (vary-meta aname assoc :arity (count fn-args))
        prefix (str "LOCAL_NEVER_CALL_DIRECTLY_" aname "--")
        gmethod-sym (symbol (str prefix "handleRequest"))
        gfullname (symbol (str (ns-name *ns*) "." aname))
        gclass (gen-class-lambda prefix gfullname)]

    `(do ~(wrap-lambda gmethod-sym fn-args fn-body gclass)
         ~(define-synthetic-name aname gmethod-sym))))

(defmacro gen-main
  {:added "0.0.1"}
  [lambdas]
  `(defn ~'-main []
     (let [fnames# (map (comp #(str (str (:ns %) "." (str (:name %)))) meta) ~lambdas)
           routes# (into {} (mapv vector fnames# ~lambdas))]
       (#'fierycod.holy-lambda.core/next-iter routes#
                                              (into {} (System/getenv))))))

(comment
  (deflambda HolyHandler
    {:doc "Holy Lambda Handler documentation"}
    [{:keys [username]} context]
    (println "Hello" username "!!!"))

  ;; (deflambda HolyHandlerAlaLambada ;; Not supported for now
  ;;   [in out ctx]
  ;;   (println "Hello"))

  (call #'HolyHandler {:username "FieryCod"}))
