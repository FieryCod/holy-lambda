(ns hello-lambda.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.retriever :as r]
   [clojure.core.async :as async]
   [fierycod.holy-lambda.interceptor :as i]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.core :as h]))

(i/definterceptor LogIncommingRequest
  {:enter (fn [request]
            (println "Log incomming request:" request)
            (async/go request))})

(h/deflambda HelloLambda <
  {:interceptors [LogIncommingRequest]}
  [request]
  (hr/json {:message "Hello"
            "it's" "me"
            :you "looking"
            :for true}))

(h/deflambda ByeLambda
  [_request]
  (hr/text "Bye bye"))

(h/deflambda RedirectLambda
  [_request]
  (hr/redirect "https://www.google.com"))

(h/deflambda AsyncLambdaFuture
  [_request]
  (future
    (println "I'm sleeping like a baby")
    (Thread/sleep 3000)
    (hr/text "Was sleeping good")))

(h/deflambda AsyncLambdaPromise
  [_request]
  (let [p (promise)]
    (future
      (println "I'm sleeping like a baby")
      (Thread/sleep 3000)
      (deliver p (hr/text "Was sleeping good")))
    p))

(h/deflambda AsyncLambdaChannel
  [_request]
  (async/go (hr/text "Yay. Channel")))

(native/entrypoint
 [#'HelloLambda
  #'RedirectLambda
  #'ByeLambda
  #'AsyncLambdaFuture
  #'AsyncLambdaPromise
  #'AsyncLambdaChannel])

(agent/in-context
 (println "In agent context"))
