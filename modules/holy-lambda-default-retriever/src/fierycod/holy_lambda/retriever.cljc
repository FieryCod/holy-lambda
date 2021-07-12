(ns ^:no-doc ^:private fierycod.holy-lambda.retriever
  "Substitutable namespace which implements basic retrieve mechanism for the payload.

  Supports:
  - Future<IPersistentMap>
  - IPersistentMap
  - Promise<IPersistentMap>

  If you looking for channel support then use:
  https://github.com/FieryCod/holy-lambda-async-retriever")

(def ^:private ^:const MAX_TIMEOUT_TIME
  "850 seconds. Max timeout is 900 for AWS Lambda"
  850000)

#?(:clj
   (defn- pending?
     [p]
     (isa? (class p) clojure.lang.IPending)))

#?(:clj
   (defn <-wait-for-response
     [response]
     (cond
       (or
        (bytes? response)
        (map? response)
        (nil? response)
        (string? response))
       response

       ;; Potentially a promise or future
       (pending? response)
       (let [timeouted? (volatile! false)
             timeout-f! (future
                          (Thread/sleep MAX_TIMEOUT_TIME)
                          (vswap! timeouted? not)
                          nil)]
         @(future
            (loop [realized-p? (realized? response)]
              (if realized-p?
                (do
                  (future-cancel timeout-f!)
                  @response)
                (if @timeouted?
                  (do
                    (when (future? response) (future-cancel response))
                    (future-cancel timeout-f!)
                    (println "[holy-lambda] IPending response timeouted. Shutting down the runtime!")
                    (System/exit -1))
                  (recur response))))))

       :else
       (do
         (println "\n\n---------------------\n[holy-lambda] Supposedly not supported response.
If you looking for channel support then exclude fierycod/holy-lambda-default-retriever in dependencies [io.github.FieryCod/holy-lambda \"VERSION\" :exclusions [io.github.FieryCod/holy-lambda-default-retriever]]
and add an extra dependency [io.github.FieryCod/holy-lambda-async-retriever \"VERSION\"]\n---------------------\n\n")
         response)))
   :cljs
   (defn <-wait-for-response
     [response]
     response))
