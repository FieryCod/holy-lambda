(ns {{main-ns}}.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

;; optionally, provide runtime specific implementations if needed
(defn say-hello
  []
  #?(:bb  (str "Hello world. Babashka is a sweet friend of mine! Babashka version: " (System/getProperty "babashka.version"))
     :clj "Hello world"))

(h/deflambda ExampleLambda
  "I can run on Java, Babashka or Native runtime..."
  [{:keys [event ctx] :as request}]

  ;; return a successful plain text response. See also, hr/json
  (hr/text (say-hello)))

;; (native + babashka only) specify the Lambda's entry point as a static main function when generating a class file
(native/entrypoint [#'ExampleLambda])

;; (native + babashka only) Executes the body in a safe agent context for native configuration generation.
;; Useful when it's hard for agent payloads to cover all logic branches.
(agent/in-context
 (println "I will help in generation of native-configurations"))
