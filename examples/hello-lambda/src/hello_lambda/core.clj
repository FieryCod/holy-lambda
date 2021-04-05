(ns hello-lambda.core
  (:gen-class)
  (:require
   [clojure.core.async :as async]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.core :as h]))

(h/deflambda HelloLambda
  [request]
  (println request)
  (hr/json {:message "Hello"
            "it's" "me"
            :you "looking"
            :for true}))

(h/deflambda ByeLambda
  [_request]
  "Bye bye")

(h/deflambda RedirectLambda
  [_request]
  (hr/redirect "https://www.google.com"))

(h/deflambda AsyncLambdaFuture
  [_request]
  (future
    (println "I'm sleeping like a baby")
    (Thread/sleep 3000)
    "Was sleeping good"))

(h/deflambda AsyncLambdaPromise
  [_request]
  (let [p (promise)]
    (future
      (println "I'm sleeping like a baby")
      (Thread/sleep 3000)
      (deliver p "Was sleeping good"))
    p))

(h/deflambda AsyncLambdaChannel
  [_request]
  (async/go "Yay. Channel"))

(h/gen-main
 [#'HelloLambda
  #'RedirectLambda
  #'ByeLambda
  #'AsyncLambdaFuture
  #'AsyncLambdaPromise
  #'AsyncLambdaChannel])
