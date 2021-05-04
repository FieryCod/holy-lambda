(ns component.example.core
  (:gen-class)
  (:require
   [com.stuartsierra.component :as component]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.interceptor :as i]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))


;; FYI: you can use `babashka.deps/add-deps`, but bear in mind that execution will be longer

(defrecord Component [x]
  component/Lifecycle
  (start [component]
    (prn :start)
    (assoc component :started true))
  (stop [component]
    (prn :stop)
    (dissoc component :started)))

(def system (component/system-map :c1 (->Component :foo)))

(component/start system)

(i/definterceptor InjectDeps
  {:enter (fn [request]
            (assoc request :deps system))})

(h/deflambda ExampleLambda <
  {:interceptors [InjectDeps]}
  [{:keys [deps event ctx]}]
  (println "DEPS" deps)
  #?(:bb
     (hr/text (str "Hello world. Babashka is sweet friend of mine! Babashka version: " (System/getProperty "babashka.version")))
     :clj (hr/text "Hello world")))

(native/entrypoint [#'ExampleLambda])

(agent/in-context
 (println "I will help in generation of native-configurations"))
