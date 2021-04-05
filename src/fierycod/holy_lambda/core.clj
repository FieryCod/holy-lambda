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
   [fierycod.holy-lambda.java-runtime :as jruntime]
   [fierycod.holy-lambda.agent]
   [fierycod.holy-lambda.util :as u])
  (:import
   [com.amazonaws.services.lambda.runtime Context]
   [java.io InputStream OutputStream]))

(def ^{:added "0.0.1"
       :arglists '([afn-sym]
                   [afn-sym request])}
  call
  "Resolves the lambda function and calls it with request map.
   Returns the callable lambda function if only one argument is passed.
   See `fierycod.holy-lambda.util/call`"
  #'fierycod.holy-lambda.util/call)

(defn- wrap-lambda
  [gmethod-sym mixin lambda gclass]
  `(do
     ~gclass
     (defn ~gmethod-sym
       ;; Arity used for testing and native runtime invocation
       ([request#]
        (~lambda request#))
       ;; Arity used for Java runtime
       ([this# ^InputStream in# ^OutputStream out# ^Context ctx#]
        (try
          (let [event# (#'fierycod.holy-lambda.util/in->edn-event in#)
                context# (#'fierycod.holy-lambda.java-runtime/java-ctx-object->ctx-edn ctx# (#'fierycod.holy-lambda.util/envs))
                response# (#'fierycod.holy-lambda.util/payload->bytes
                           (~lambda {:event event#
                                     :ctx context#}))]
            (.write out# ^"[B" response#))
          (catch Exception error#
            (println "[Holy Lambda] Exception during request handling" error#))
          (finally
            (.close out#)))))))

(defn- fn-body? [form]
  (when (and (seq? form)
             (vector? (first form)))
    (if (= '< (second form))
      (throw (IllegalArgumentException. "Mixins must be given before argument list"))
      true)))

(defn >parse-deflambda
  ":name  :doc?  <? :mixin* :body+
   symbol string <  expr   fn-body"
  [attrs]
  (when-not (instance? clojure.lang.Symbol (first attrs))
    (throw (IllegalArgumentException. "First argument to deflambda must be a symbol")))
  (loop [res  {}
         xs   attrs
         mode nil]
    (let [x    (first xs)
          anext (next xs)]
      (cond
        (and (empty? res) (symbol? x)) (recur {:lname x} anext nil)
        (fn-body? xs)        (assoc res :bodies (list xs))
        (every? fn-body? xs) (assoc res :bodies xs)
        (string? x)          (recur (assoc res :doc x) anext nil)
        (= '< x)             (recur res anext :mixin)
        (= mode :mixin) (recur (assoc res :mixin x) anext nil)
        :else (throw (IllegalArgumentException. (str "Syntax error at " xs)))))))

(defmacro deflambda
  "Convenience macro for generating defn alike lambdas."
  {:arglists '([name doc-string? <mixin-sign? mixin? [request] fn-body])
   :added "0.0.1"}
  [& attrs]
  (let [{:keys [mixin lname bodies doc]} (>parse-deflambda attrs)
        arglist (mapv (fn [[arglist & _body]] arglist) bodies)
        lname (with-meta lname {:doc doc
                                :public-var? true
                                :arglists `(quote ~arglist)})
        prefix (str "PRVL" lname "--")
        gmethod-sym (symbol (str prefix "handleRequest"))
        gfullname (symbol (str (ns-name *ns*) "." lname))
        gclass (jruntime/gen-lambda-class-with-prefix prefix gfullname)
        lambda `(fn ~@bodies)]

    `(do ~(wrap-lambda gmethod-sym mixin lambda gclass)
         (def ~lname ~gmethod-sym))))

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
  `(do
     (def ~'PRVL_ROUTES (into {} (mapv (fn [l#] [(str (str (:ns (meta l#)) "." (str (:name (meta l#))))) l#]) ~lambdas)))
     (defn ~'-main []
       ;; executor = native-agent    -- Indicates that the configuration for compiling via `native-image`
       ;;                               will be generated via the agent.
       ;;                               Example in: `examples/sqs-example/Makefile` at `gen-native-configuration` command
       ;;
       ;; executor = anything else   -- Run provided runtime loop
       (if (= (System/getProperty "executor") "native-agent")
         ;; Generate the native configuration for the lambdas
         (#'fierycod.holy-lambda.agent/routes->reflective-call! ~'PRVL_ROUTES)

         ;; Start native runtime loop
         (while true
           (#'fierycod.holy-lambda.native-runtime/next-iter ~'PRVL_ROUTES (#'fierycod.holy-lambda.util/envs)))))))
