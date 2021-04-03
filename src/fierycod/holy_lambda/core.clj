(ns fierycod.holy-lambda.core
  "This namespace integrates the Clojure code with two different runtimes: Java Lambda Runtime, Native Provided Runtime.
  The former is the Official Java Runtime for AWS Lambda which is well tested and works perfectly fine, but it's rather slow due to cold starts.
  The latter is a custom [runtime](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html) integrated within the framework.
  It's a significantly faster than the Java runtime due to the use of GraalVM.

  *Namespace includes:*
  - Friendly macro for generating Lambda functions which run on both runtimes
  - TODO Utilities which help produce valid response"
  (:require
   [fierycod.holy-lambda.native-runtime]
   [fierycod.holy-lambda.agent]
   [fierycod.holy-lambda.util :as u]
   [clojure.data.json :as json]
   [clojure.tools.macro :as macro])
  (:import
   [java.io InputStream OutputStream]
   [com.amazonaws.services.lambda.runtime Context CognitoIdentity ClientContext Client]))

(def ^{:added "0.0.1"
       :arglists '([afn-sym]
                   [afn-sym request])}
  call
  "Resolves the lambda function and calls it with the event and context.
  Returns the callable lambda function if only one argument is passed.
  See `fierycod.holy-lambda.util/call`"
  #'fierycod.holy-lambda.util/call)

(defn- gen-class-lambda
  [prefix gfullname]
  `(gen-class
    :name ~gfullname
    :prefix ~prefix
    :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))

(defn- java-ctx-object->ctx-edn
  [^Context context envs]
  (u/ctx envs
         (fn [] (.getRemainingTimeInMillis context))
         (.getFunctionName context)
         (.getFunctionVersion context)
         (.getInvokedFunctionArn context)
         (.getMemoryLimitInMB context)
         (.getAwsRequestId context)
         (.getLogGroupName context)
         (.getLogStreamName context)
         (when-let [^CognitoIdentity _identity (.getIdentity context)]
           {:identityId (.getIdentityId _identity)
            :identityPoolId (.getIdentityPoolId _identity)})
         (when-let [^ClientContext client-context (.getClientContext context)]
           {:client (when-let [^Client client (.getClient client-context)]
                      {:installationId (.getInstallationId client)
                       :appTitle (.getAppTitle client)
                       :appVersionName (.getAppVersionName client)
                       :appVersionCode (.getAppVersionCode client)
                       :appPackageName (.getAppPackageName client)})
            :custom (into {} (.getCustom client-context))
            :environment (into {} (.getEnvironment client-context))})))

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
          (let [event# (#'fierycod.holy-lambda.util/in->edn-event in#)
                context# (#'fierycod.holy-lambda.core/java-ctx-object->ctx-edn ctx# (#'fierycod.holy-lambda.util/envs))
                response# (~lambda {:event event#
                                    :ctx context#})
                f-response# (assoc response# :body (json/write-str (:body response#)))]
            (.write out# (.getBytes ^String (json/write-str f-response#) "UTF-8"))))))))

(defmacro deflambda
  "Convenience macro for generating defn alike lambdas."
  {:arglists '([name doc-string? attrs-map? [reqeust] prepost-map? fn-body])
   :added "0.0.1"}
  [aname & attrs]
  (let [[aname [fn-args & fn-body]] (macro/name-with-attributes aname attrs)
        prefix (str "PRVL" aname "--")
        gmethod-sym (symbol (str prefix "handleRequest"))
        gfullname (symbol (str (ns-name *ns*) "." aname))
        gclass (gen-class-lambda prefix gfullname)]

    `(do ~(wrap-lambda gmethod-sym fn-args fn-body gclass)
         (def ~aname ~gmethod-sym))))

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
         (#'fierycod.holy-lambda.agent/routes->reflective-call! routes#)

         ;; Otherwise just start the runtime loop
         (while true
           (#'fierycod.holy-lambda.native-runtime/next-iter routes# (#'fierycod.holy-lambda.util/envs)))))))
