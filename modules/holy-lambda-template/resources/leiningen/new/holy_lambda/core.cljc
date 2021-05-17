(ns {{main-ns}} .core
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


;; An interceptor is a pair of unary functions representing inbound requests and outbound responses.
;;
;; holy-lambda calls the :enter function on the way "in" to handling a request. It calls the :leave function on the way back "out".
;;
;; The :enter function is called with the request map and must return either a request map or a channel that will deliver a request map.
;; Similarly, the :leave function is called with the response map and must return either a response map or a channel that will deliver a response map.
;;
;; Interceptors may be chained together:
;;     < {:interceptors [Interceptor1 Interceptor2]}
;;
;; Execution order is as left to right in both inbound AND outbound flows, on the way in:
;;   Interceptor1 :enter, Interceptor2 :enter
;;
;; On the way out:
;;   Interceptor1 :leave, Interceptor2 :leave

(i/definterceptor LogIncomingRequest
                  {:enter (fn [request]
                            (println request)
                            request)})

(i/definterceptor AddHeaderToResponse
                  {:leave (fn [response]
                            (hr/header response "Custom-Header" "Some Value"))})



(h/deflambda ExampleLambda
             "I can run on Java, Babashka or Native runtime..."
             < {:interceptors [LogIncomingRequest AddHeaderToResponse]}
             [{:keys [event ctx] :as request}]

             ;; return a successful plain text response. See also, hr/json
             (hr/text (say-hello)))


;; (native only) specify the Lambda's entry point as a static main function when generating a class file
(native/entrypoint [#'ExampleLambda])


;; (native only) Executes the body in a safe agent context for native configuration generation.
;; Useful when it's hard for agent payloads to cover all logic branches.

(agent/in-context
  (println "I will help in generation of native-configurations"))
