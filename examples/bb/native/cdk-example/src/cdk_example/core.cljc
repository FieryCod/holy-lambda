(ns cdk-example.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.interceptor :as i]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

;; optionally, provide runtime specific implementations if needed
(defn say-hello
  []
  #?(:bb  (str "Hello world. Babashka is a sweet friend of mine! Babashka version: " (System/getProperty "babashka.version"))
     :clj "Hello world"))

;; (alpha feature) An interceptor is a single or pair of unary functions representing inbound requests and/or outbound
;; responses, allowing code to inspect or amend requests/responses between AWS and your Lambda function.
;; See here https://cljdoc.org/d/fierycod/holy-lambda/CURRENT/doc/features/interceptors

(i/definterceptor LambdaLogger
  {:enter (fn [request]
            (println "REQUEST:" request)
            request)
   :leave (fn [response]
            (println "RESPONSE:" response)
            response)})

(i/definterceptor AddHeaderToResponse
  {:leave (fn [response]
            (hr/header response "Custom-Header" "Some Value"))})

(h/deflambda ExampleLambda
  "I can run on Java, Babashka or Native runtime..."
  < {:interceptors [LambdaLogger AddHeaderToResponse]}
  [{:keys [event ctx] :as request}]

  ;; return a successful plain text response. See also, hr/json
  (hr/text (say-hello)))

;; (native only) specify the Lambda's entry point as a static main function when generating a class file
(native/entrypoint [#'ExampleLambda])

;; (native only) Executes the body in a safe agent context for native configuration generation.
;; Useful when it's hard for agent payloads to cover all logic branches.

(agent/in-context
 (println "I will help in generation of native-configurations"))
