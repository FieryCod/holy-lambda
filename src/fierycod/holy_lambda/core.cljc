(ns fierycod.holy-lambda.core
  (:require
   [fierycod.holy-lambda.util]
   [fierycod.holy-lambda.custom-runtime]
   [fierycod.holy-lambda.agent]))

(defmacro entrypoint
  "Generates the `-main` function, which is an entrypoint for `bootstrap` script.
  Lambdas passed as a first parameter are converted to a routing map {`HANDLER_NAME` -> `HANDLER_FN`}.
  Routing is persisted in static `PRVL_ROUTES` var that is later used via a AWS Lambda Custom runtime
  to match the `HANDLER_NAME` with a corresponding `HANDLER_FN`.

  Generated `-main` function might take the optional `HANDLER_NAME` passed as first argument, that overrides the `HANDLER_NAME` passed as
  `_HANDLER` in environment context.

  1. The `-main` might be called by `AWS Lambda`.
  2. The `-main` might be called by `native-agent` to generate the `native-configuration` necessary to compile the project to native (native backend).

      **For more info take a look into the corresponding links:**
      1. https://github.com/oracle/graal/issues/1367
      2. https://github.com/oracle/graal/blob/master/substratevm/CONFIGURE.md
      3. https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md

      According to the comment of the @cstancu with the help of the agent we can find the majority of the reflective calls and generate the configuration.
      Generated configuration might then be used by `native-image` tool.

  **Options:**
  - `init-hook` - Side effect function with no arguments that is run before the runtime loop starts. Useful for initialization of the outer state.
  - `disable-analytics?` - Boolean for disabling the basic information (Runtime + Java/Babashka version) send via `UserAgent` header AWS API.

  **Usage:**
  ```clojure
  (ns some-ns
    (:require
      [fierycod.holy-lambda.core :as h]))

  (defn ExampleLambda1
    [request]
    (hr/text \"Hello world!\"))

  (defn ExampleLambda2
    [request]
    (hr/text \"Hello world v2!\"))

  (h/entrypoint [#'ExampleLambda1 #'ExampleLambda2])
  ```
  "
  {:added "0.0.1"}
  [lambdas & [{:keys [init-hook disable-analytics?]
               :or   {init-hook          (fn [] nil)
                      disable-analytics? false}}]]
  `(do
     (def ~'PRVL_ROUTES (into {} (mapv (fn [l#] [(str (str (:ns (meta l#)) "." (str (:name (meta l#))))) l#]) ~lambdas)))
     (defn ~'-main [& attrs#]
       (let [runtime-api-url# (System/getenv "AWS_LAMBDA_RUNTIME_API")
             handler-name#    (or (first attrs#) (System/getenv "_HANDLER"))]

         ;; Side effect init-hook
         (~init-hook)

         ;; executor = native-agent    -- Indicates that the configuration for compiling via `native-image`
         ;;                               will be generated via the agent.
         ;;
         ;; executor = anything else   -- Run provided runtime loop
         (if (= (System/getProperty "executor") @#'fierycod.holy-lambda.agent/AGENT_EXECUTOR)
           ;; Generate the native configuration for the lambdas
           (#'fierycod.holy-lambda.agent/routes->reflective-call! ~'PRVL_ROUTES)

           ;; Start custom runtime loop
           (while true
             (#'fierycod.holy-lambda.custom-runtime/next-iter
              runtime-api-url#
              handler-name#
              ~'PRVL_ROUTES
              ~disable-analytics?)))))))
