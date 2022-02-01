(ns ^:no-doc ^:private fierycod.holy-lambda.retriever
  (:require
   [clojure.core.async :as async])
  (:import
   [clojure.core.async.impl.channels ManyToManyChannel]))

#?(:clj
   (defn- chan?
      [?c]
      (instance? ManyToManyChannel ?c)))

#?(:clj
   (defn- pending?
     [p]
     (instance? clojure.lang.IPending p)))

#?(:clj
   (defn <-wait-for-response
     [response]
     (if (chan? response)
       (async/<!! response)
       (if (pending? response)
         @response
         response)))
   :cljs
   (defn <-wait-for-response
     [response]
     response))
