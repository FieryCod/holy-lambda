(ns aws-pod.example.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda-ring-adapter.core :as hlra]
   [babashka.pods :as pods]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

(def handler (hlra/wrap-hl-req-res-model identity))

(def region "eu-central-1")

(pods/load-pod 'org.babashka/aws "0.0.6")

(require '[pod.babashka.aws :as aws])

(def s3-client
  (aws/client {:api :s3 :region region}))

(defn ExampleLambda
  [{:keys [event ctx]}]
  #?(:bb
     (do
       (hr/text (boolean (aws/invoke s3-client {:op :ListBuckets}))))
     :clj (hr/text "Hello world")))

(h/entrypoint [#'ExampleLambda])

(agent/in-context
 (println "I will help in generation of native-configurations"))
