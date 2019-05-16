(ns hello-lambda.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h]))

(h/deflambda HelloLambda
  [event context]
  {:statusCode 200
   :body {:message "Hello"
          "it's" "me"
          :you "looking"
          :for true}
   :isBase64Encoded false
   :headers {"Content-Type" "application/json"}})

(h/gen-main [#'HelloLambda])
