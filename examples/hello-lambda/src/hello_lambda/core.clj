(ns hello-lambda.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h]))

(h/deflambda HelloLambda
  [event context]
  {:statusCode 200
   :body "Hello"
   :isBase64Encoded false
   :headers {"content-type" "text/html"}}
  )

(h/gen-main [#'HelloLambda])
