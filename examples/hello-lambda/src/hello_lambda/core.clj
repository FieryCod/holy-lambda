(ns hello-lambda.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h]))

(h/deflambda HelloLambda
  [event context]
  (h/info "Logging...")
  {:statusCode 200
   :body {:message "Hello"
          "it's" "me"
          :you "looking"
          :for true}
   :isBase64Encoded false
   :headers {"Content-Type" "application/json"}})

(h/deflambda ByeLambda
  [event context]
  {:statusCode 200
   :body "Bye bye"
   :isBase64Encoded false
   :headers {"Content-Type" "application/json"}})

(h/gen-main [#'HelloLambda
             #'ByeLambda])
