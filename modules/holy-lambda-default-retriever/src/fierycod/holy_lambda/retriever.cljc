(ns ^:no-doc ^:private fierycod.holy-lambda.retriever
  "Substitutable namespace which implements basic retrieve mechanism for the payload.

  Supports:
  - Future<IPersistentMap>
  - IPersistentMap
  - Promise<IPersistentMap>

  If you looking for channel support then use:
  https://github.com/FieryCod/holy-lambda-async-retriever")

#?(:clj
   (defn- pending?
     [p]
     (instance? clojure.lang.IPending p)))

#?(:clj
   (defn <-wait-for-response
     [response]
     (if (pending? response)
       @response
       response))
   :cljs
   (defn <-wait-for-response
     [response]
     response))
