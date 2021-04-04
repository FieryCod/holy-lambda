(ns hello-lambda.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h]))

(h/deflambda HelloLambda
  [request]
  (println request)
  {:statusCode 200
   :body {:message "Hello"
          "it's" "me"
          :you "looking"
          :for true}
   :isBase64Encoded false
   :headers {"Content-Type" "application/json"}})

(h/deflambda ByeLambda
  [_request]
  "Bye bye")

(h/gen-main [#'HelloLambda
             #'ByeLambda])
