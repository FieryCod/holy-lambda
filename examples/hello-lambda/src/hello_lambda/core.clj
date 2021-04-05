(ns hello-lambda.core
  (:gen-class)
  (:require
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

(h/gen-main [#'HelloLambda
             #'RedirectLambda
             #'ByeLambda])
