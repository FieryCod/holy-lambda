(ns fierycod.holy-lambda.core
  (:require
   [fierycod.holy-lambda.util]
   [fierycod.holy-lambda.custom-runtime]
   [fierycod.holy-lambda.agent]))

(defmacro entrypoint
  "Generates the `-main` function, which is an entrypoint for `bootstrap` script.
  Lambdas passed as a first parameter are converted to a routing map {`HANDLER_NAME` -> `HANDLER_FN`}. Routing is persisted in static `PRVL_ROUTES` var
  that is later used via a AWS Lambda Custom runtime to match the `HANDLER_NAME` with a corresponding `HANDLER_FN`.

  Generated `-main` function might take the optional `HANDLER_NAME` passed as first argument, that overrides the `HANDLER_NAME` passed as
  `_HANDLER` in environment context.

  **Options:**
  - `init-hook` - Side effect function with no arguments that is run before the runtime loop starts. Useful for initialization of the outer state.
  - `disable-analytics?` - Boolean for disabling the basic information (Runtime + Java version) send via `UserAgent` header AWS API.
  "
  {:added "0.0.1"}
  [lambdas & [{:keys [init-hook disable-analytics?]
               :or {init-hook (fn [] nil)
                    disable-analytics? false}}]]
  `(do
     (def ~'PRVL_ROUTES (into {} (mapv (fn [l#] [(str (str (:ns (meta l#)) "." (str (:name (meta l#))))) l#]) ~lambdas)))
     (defn ~'-main [& attrs#]

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
            (System/getenv "AWS_LAMBDA_RUNTIME_API")
            (or (first attrs#) (System/getenv "_HANDLER"))
            ~'PRVL_ROUTES
            ~disable-analytics?))))))
