(ns {{name}}.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

(h/deflambda ExampleLambda <
  [{:keys [event ctx]}]
  (hr/text "Hello world"))

(native/entrypoint [#'ExampleLambda])

(agent/in-context
 (println "I will help in generation of native-configurations"))
