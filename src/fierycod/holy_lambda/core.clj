(ns fierycod.holy-lambda.core
  ^{:doc "This namespace integrates the Clojure code with two different runtimes: Java Lambda Runtime, Native Provided Runtime.
          The former is the Official Java Runtime for AWS Lambda which is well tested and works perfectly fine, but it's rather slow due to cold starts.
          The latter is a custom [runtime](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html) made by me.
          It's a significantly faster than the Java runtime due to the use of GraalVM.

          *Namespace includes:*
          - Utilities for Logging
          - Friendly macro for generating Lambda functions which run on both runtimes
          - TODO Utilities which help produce valid response"
    :author "Karol Wójcik"}
  (:require
   [clojure.string :as string]
   [clojure.data.json :as json]
   [clojure.tools.macro :as macro])
  (:import
   [java.time Instant]
   [java.util Date]
   [java.net URL HttpURLConnection]
   [java.io InputStream OutputStream InputStreamReader]
   [com.amazonaws.services.lambda.runtime
    RequestStreamHandler Context CognitoIdentity ClientContext Client LambdaLogger]))

(defn- logger-factory
  [& [logger-impl]]
  (let [unified-logger (proxy [LambdaLogger] []
                         (log [s]
                           (println s)))]
    (or logger-impl unified-logger)))

(defn- ^String decorate-log
  [severity vvs]
  (str (condp = severity
         :log ""
         :info "[INFO] "
         :warn "[WARN] "
         :error "[ERROR] "
         :fatal "[FATAL] "
         :else "")  ;; native runtime error
       (string/join " " vvs)))

(def ^:dynamic ^:private runtime nil)
(def ^:dynamic ^:private invocation-id nil)
(def ^:dynamic ^:private ^LambdaLogger logger (logger-factory))
(def ^:private success-codes #{200 202})

(defn log
  [& vs]
  (.log logger (decorate-log :log vs)))

(defn info
  [& vs]
  (.log logger (decorate-log :info vs)))

(defn warn
  [& vs]
  (.log logger (decorate-log :warn vs)))

(defn error
  [& vs]
  (.log logger (decorate-log :error vs)))

(defn- fatal
  [& vs]
  (.log logger (decorate-log :fatal vs)))

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
  [envs rem-time fn-name fn-version fn-invoked-arn memory-limit
   aws-request-id log-group-name log-stream-name
   identity client-context logger-impl]
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
   :envs envs
   :logger logger-impl})

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
          :custom (into {} (.getCustom client-context))
          :environment (into {} (.getEnvironment client-context))})
       (.getLogger context)))

(defn- in->edn-event
  [^InputStream event]
  (json/read (InputStreamReader. event "UTF-8") :key-fn keyword))

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
           ;; Arity used for testing and native runtime invocation
           ([event# context#]
            (~lambda event# context#))
           ;; Arity used for Java runtime
           ([this# ^InputStream in# ^OutputStream out# ^Context ctx#]
            (binding [logger (#'fierycod.holy-lambda.core/logger-factory (.getLogger ctx#))]
              (let [event# (#'fierycod.holy-lambda.core/in->edn-event in#)
                    context# (#'fierycod.holy-lambda.core/ctx-object->ctx-edn ctx# (into {} (System/getenv)))
                    response# (~lambda event# context#)
                    f-response# (assoc response# :body (json/write-str (:body response#)))]
                (.write out# (.getBytes ^String (json/write-str f-response#) "UTF-8")))))))
      3
      ;; TODO: Check whether lambada style would be helpful

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

      ;; When arity does not match
      (throw (Exception. "Invalid arity..")))))

(defn- native->aws-context
  [headers event env-vars]
  (let [get-env (partial get env-vars)
        getf-header (getf-header* headers)]
    (ctx env-vars (- (Long/parseLong (getf-header "Lambda-Runtime-Deadline-Ms")) (System/currentTimeMillis))
         (get-env "AWS_LAMBDA_FUNCTION_NAME")
         (get-env "AWS_LAMBDA_FUNCTION_VERSION")
         (str "arn:aws:lambda:" (get-env "AWS_REGION")
              ":" (get-in event [:requestContext :accountId] "0000000")
              ":function:" (get-env "AWS_LAMBDA_FUNCTION_NAME"))
         (get-env "AWS_LAMBDA_FUNCTION_MEMORY_SIZE")
         ;; TODO: Incorrect requestId
         (-> event :requestContext :requestId)
         (get-env "AWS_LAMBDA_LOG_GROUP_NAME")
         (get-env "AWS_LAMBDA_LOG_STREAM_NAME")
         ;; #8
         {:identityId "????"
          :identityPoolId (-> event :requestContext :identity :cognitoIdentityPoolId)}
         ;; #7
         {:client nil :custom nil :environment nil}
         logger)))

(defn- retrieve-body
  [^HttpURLConnection http-conn status]
  (if-not (success-codes status)
    (.getErrorStream http-conn)
    (.getInputStream http-conn)))

(defn- http
  "Internal http method which sends/receive data from AWS"
  [method url-s & [payload]]
  (let [push? (= method "POST")
        ^String payload-s (when push? (if (string? payload) payload
                                          (json/write-str (assoc payload
                                                                 :body (json/write-str (:body payload))))))
        ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
        _ (doto http-conn
            (.setDoOutput push?)
            (.setRequestProperty "Content-Type" "application/json")
            (.setRequestMethod method))
        _ (when push?
            (doto (.getOutputStream http-conn)
              (.write (.getBytes payload-s "UTF-8"))
              (.close)))
        headers (into {} (.getHeaderFields http-conn))
        status (.getResponseCode http-conn)]
    {:headers headers
     :status status
     :body (in->edn-event (retrieve-body http-conn status))}))

(defn- send-runtime-error
  [^Exception err]
  (let [exit! #(System/exit -1)
        url (str "http://" runtime "/2018-06-01/runtime/invocation/" invocation-id "/error")
        payload {:errorMessage (.getMessage err)
                 :errorType (-> err (.getClass) (.getCanonicalName))}
        response (http "POST" url payload)]
    (error (.getMessage err))
    (when-not (success-codes (:status response))
      (do (fatal "AWS did not accept the response. Error message: " (:body response))
          (exit!)))))

(defn- fetch-aws-event
  [runtime]
  (let [url (str "http://" runtime "/2018-06-01/runtime/invocation/next")
        aws-event (http "GET" url)]
    (assoc aws-event :invocation-id (getf-header* (:headers aws-event) "Lambda-Runtime-Aws-Request-Id"))))

(defn call
  "Resolves the lambda function and calls it with the event and context.
   Returns the callable lambda function if only one argument is passed."
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
  (let [url (str "http://" runtime "/2018-06-01/runtime/invocation/" invocation-id "/response")
        {:keys [status body]} (http "POST" url response)]
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
        invocation-id* (:invocation-id aws-event)
        handler (get routes handler-name)]
    (binding [runtime runtime*
              logger (logger-factory)
              invocation-id invocation-id*]
      (if-not handler
        (send-runtime-error (Exception. (str "Handler: " handler-name " not found!")))
        (when (and invocation-id (success-codes (:status aws-event)))
          (process-event aws-event env-vars (call handler)))))))

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
        prefix (str "LOCAL_NEVER_CALL_DIRECTLY_" aname "--")
        gmethod-sym (symbol (str prefix "handleRequest"))
        gfullname (symbol (str (ns-name *ns*) "." aname))
        gclass (gen-class-lambda prefix gfullname)]

    `(do ~(wrap-lambda gmethod-sym fn-args fn-body gclass)
         ~(define-synthetic-name aname gmethod-sym))))

(defmacro gen-main
  "Generates the main function. The `-main` is then used by AWS to run Custom runtime
  which then proxies function names to corresponding handler "
  {:added "0.0.1"}
  [lambdas]
  `(defn ~'-main []
     (let [fnames# (map (comp #(str (str (:ns %) "." (str (:name %)))) meta) ~lambdas)
           routes# (into {} (mapv vector fnames# ~lambdas))]
       (while true
         (#'fierycod.holy-lambda.core/next-iter routes#
                                                (into {} (System/getenv)))))))

(comment
  (deflambda HolyHandler
    {:doc "Holy Lambda Handler documentation"}
    [{:keys [username]} context]
    (println "Hello" username "!!!"))

  ;; (deflambda HolyHandlerAlaLambada ;; Not supported for now
  ;;   [in out ctx]
  ;;   (println "Hello"))

  (call #'HolyHandler {:username "FieryCod"} nil))
