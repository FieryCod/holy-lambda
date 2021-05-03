(ns component.example.core
  (:gen-class)
  (:require
   ;; [com.stuartsierra.component :as component]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.native-runtime :as native]
   [babashka.process :as p]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

(defn- shell
  [cmd & args]
  (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit true}))

;; (shell "ls -la /opt")

(h/deflambda ExampleLambda <
  [{:keys [event ctx]}]
  #?(:bb
     (hr/text (str "Hello world. Babashka is sweet friend of mine! Babashka version: " (System/getProperty "babashka.version")))
     :clj (hr/text "Hello world")))

(native/entrypoint [#'ExampleLambda])

(agent/in-context
 (println "I will help in generation of native-configurations"))
