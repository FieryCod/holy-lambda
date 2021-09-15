(ns fierycod.holy-lambda.core
  (:require
   [fierycod.holy-lambda.util]
   [fierycod.holy-lambda.custom-runtime]
   [fierycod.holy-lambda.agent]))

(defmacro entrypoint
  "Generates the `-main` function that executes Clojure functions upon `AWS Lambda` event. Takes care of producing valid `AWS Lambda` routing. See docs for more."
  {:added "0.0.1"}
  [lambdas & [{:keys [init-hook] :or {init-hook (fn [] nil)}}]]
  `(do
     (def ~'PRVL_ROUTES (into {} (mapv (fn [l#] [(str (str (:ns (meta l#)) "." (str (:name (meta l#))))) l#]) ~lambdas)))
     (defn ~'-main [& attrs#]
       (let [maybe-handler-name# (first attrs#)
             envs# (#'fierycod.holy-lambda.util/adopt-map (System/getenv))]
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
             (#'fierycod.holy-lambda.custom-runtime/next-iter maybe-handler-name# ~'PRVL_ROUTES envs#)))))))
