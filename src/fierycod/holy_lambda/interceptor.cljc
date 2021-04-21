(ns fierycod.holy-lambda.interceptor
  (:require
   [fierycod.holy-lambda.retriever :as retriever]))

(defn- forms->def
  ([asym form]
   (forms->def asym "" form))
  ([asym ?m-docstring ?form]
   (let [docstring (if (string? ?m-docstring) ?m-docstring "")
         form (if (string? ?m-docstring)
                ?form
                (concat [?m-docstring] [?form]))]
   [asym docstring form]))
  ([asym ?m-docstring ?form & ?forms]
   (let [docstring (if (string? ?m-docstring) ?m-docstring "")
         form (if (string? ?m-docstring)
                (concat [?form] ?forms)
                (concat [?m-docstring] [?form] ?forms))]
     [asym docstring form])))

(defn- wrap-interceptor
  [?sym ?handler ?type]
  `(if (not (fn? ~?handler))
    (throw (Exception. (str "Entry " ~?type " for interceptor \"" ~?sym "\" should be a function.")))
    (fn [payload#]
      (try
        (update-in (retriever/<-wait-for-response (~?handler payload#))
                   [::interceptors :complete ~?type]
                   (fnil conj [])
                   ~?sym)
        (catch Exception ex#
          (update-in payload#
                     [::interceptors :ex]
                     (fnil conj [])
                     {:ex ex#
                      :interceptor ~?sym
                      :type ~?type}))))))

(defmacro definterceptor
  "Defines an interceptor out of map {:leave (fn [response] response)? :enter (fn [request] request)?}. Interceptors should be passed to lambda as a mixin.

  Usage:

  ```
  (definterceptor ExampleInterceptor
    {:leave (fn [response] (hr/origin response \"*\"))
     :enter (fn [request] (println \"I will log a request\" request) request)})
  ```"
  [?sym & ?attrs]
  (let [[?sym ?docstring ?body] (apply forms->def ?sym ?attrs)]
    (if (or (nil? ?body)
            (not (map? ?body))
            (and (nil? (:enter ?body))
                 (nil? (:leave ?body))))
      (throw (IllegalArgumentException. "Interceptor should be an map of: [:enter (fn [request] request)]? and [:leave (fn [response] response)]?"))
      (let [{:keys [enter leave]} ?body
            wrapped-enter (if-not enter
                            nil
                            (wrap-interceptor (str ?sym) enter :enter))
            wrapped-leave (if-not leave
                            nil
                            (wrap-interceptor (str ?sym) leave :leave))
            interceptor `{:enter ~wrapped-enter
                          :leave ~wrapped-leave}]
        `(def ~?sym ~?docstring ~interceptor)))))

#?(:bb
   (defn- process-interceptors
     [mixin payload type]
     (if-let [interceptors (seq (:interceptors mixin))]
       (loop [interceptors interceptors
              result payload]
         (if-not (seq interceptors)
           result
           (recur (rest interceptors) (if-let [interceptor (type (first interceptors))] (interceptor result) result))))
       payload))
   :cljs
   (defn- process-interceptors
     [mixin payload type]
     (if-let [interceptors (seq (:interceptors mixin))]
       (loop [interceptors interceptors
              result payload]
         (if-not (seq interceptors)
           result
           (recur (rest interceptors) (if-let [interceptor (type (first interceptors))] (interceptor result) result))))
       payload))
   :clj
   (defn- process-interceptors
     [?mixin ?payload ?type]
     (if-let [it (some-> ?mixin :interceptors clojure.lang.RT/iter)]
       (loop [result ?payload]
         (if (.hasNext it)
           (recur
            (if-let [interceptor (some-> (.next it) ?type)]
              (interceptor result)
              result))
           result))
       ?payload)))
