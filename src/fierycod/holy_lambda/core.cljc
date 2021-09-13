(ns fierycod.holy-lambda.core
  (:require
   [fierycod.holy-lambda.custom-runtime]
   [fierycod.holy-lambda.agent]))

(defmacro entrypoint
 "Generates the entrypoint function which has the two roles:

1. The `-main` might be then launched by AWS in the lambda runtime.
   Lambda runtime tries to proxy the payloads from AWS to corresponding handlers.

2. The `-main` might be used to generate the configuration necessary to compile the project to native.

    **For more info take a look into the corresponding links:**
    1. https://github.com/oracle/graal/issues/1367
    2. https://github.com/oracle/graal/blob/master/substratevm/CONFIGURE.md
    3. https://github.com/oracle/graal/blob/master/substratevm/REFLECTION.md

    According to the comment of the @cstancu with the help of the agent we can find the majority
    of the reflective calls and generate the configuration. Generated configuration might then be used
    by `native-image` tool.

**Usage**:

```clojure
  (defn ExampleLambda1
    [request]
    (hr/text \"Hello world!\"))

  (defn ExampleLambda2
    [request]
    (hr/text \"Hello world v2!\"))

  (entrypoint [#'ExampleLambda1 #'ExampleLambda2])
```
"
  {:added "0.0.1"}
  [lambdas & [{:keys [init-hook] :or {init-hook (fn [] nil)}}]]
  `(do
     (def ~'PRVL_ROUTES (into {} (mapv (fn [l#] [(str (str (:ns (meta l#)) "." (str (:name (meta l#))))) l#]) ~lambdas)))
     (defn ~'-main [& attrs#]
       (when (fn? ~init-hook)
         (~init-hook))

       ;; executor = native-agent    -- Indicates that the configuration for compiling via `native-image`
       ;;                               will be generated via the agent.
       ;;
       ;; executor = anything else   -- Run provided runtime loop
       (if (= (System/getProperty "executor") @#'fierycod.holy-lambda.agent/AGENT_EXECUTOR)
         ;; Generate the native configuration for the lambdas
         (#'fierycod.holy-lambda.agent/routes->reflective-call! ~'PRVL_ROUTES)

         ;; Start custom runtime loop
         (while true
           (#'fierycod.holy-lambda.custom-runtime/next-iter (first attrs#) ~'PRVL_ROUTES (into {} (System/getenv))))))))
