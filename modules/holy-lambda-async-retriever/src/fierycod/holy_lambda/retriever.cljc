(ns ^:no-doc ^:private fierycod.holy-lambda.retriever
  "Substitutable namespace which implements basic retrieve mechanism for the payload.

  Supports:
  - Future<IPersistentMap>
  - IPersistentMap
  - Promise<IPersistentMap>
  - Channel<IPersistentMap> "
  (:require
   [clojure.core.async :as async])
  (:import
   [clojure.core.async.impl.channels ManyToManyChannel]))

#?(:clj
   (defn- chan?
      [?c]
      (instance? ManyToManyChannel ?c)))

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

       (chan? response)
       (<-wait-for-response (future (async/<!! response)))

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
         (println "\n\n---------------------\n[holy-lambda] Supposedly not supported response.\n---------------------\n\n")
         response)))
   :cljs
   (defn <-wait-for-response
     [response]
     response))
