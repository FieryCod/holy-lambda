(ns aws-pod.example.core
  (:gen-class)
  (:require
   #?(:bb
      [babashka.pods :as pods])
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

(def region "eu-central-1")

#?(:bb
   (pods/load-pod 'org.babashka/aws "0.0.5"))

#?(:bb
   (require '[pod.babashka.aws :as aws]))

#?(:bb
   (def s3-client
     (aws/client {:api :s3 :region region})))

(h/deflambda ExampleLambda <
  [{:keys [event ctx]}]
  #?(:bb
     (do
       (println (aws/doc s3-client :ListBuckets))
       (println (aws/invoke s3-client {:op :ListBuckets}))
       (hr/text (str "Hello world. Babashka is sweet friend of mine! Babashka version: " (System/getProperty "babashka.version"))))
     :clj (hr/text "Hello world")))

(native/entrypoint [#'ExampleLambda])

(agent/in-context
 (println "I will help in generation of native-configurations"))
