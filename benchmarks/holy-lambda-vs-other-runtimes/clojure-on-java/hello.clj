(ns hello
  (:require
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.core :as h])
  (:gen-class))

(h/deflambda Hello <
  [_]
  (hr/text "Hello world!"))

