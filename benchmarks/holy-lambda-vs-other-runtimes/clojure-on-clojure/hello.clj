(ns hello
  (:require
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.core :as h])
  (:gen-class))

(h/deflambda Hello <
  [_]
  (hr/text "Hello world!"))

(native/entrypoint [#'Hello])
