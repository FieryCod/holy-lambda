(ns sqs-example.core
  (:gen-class)
  (:require
   [sqs-example.static-load]
   [cognitect.aws.http.cognitect :as http]
   [cognitect.aws.client.api :as aws]
   [fierycod.holy-lambda.core :as h]))

(def http-client (delay (http/create)))
(def sqs (delay (aws/client {:api :sqs
                             :http-client @http-client})))

(h/deflambda SubscribeLambda
  [event context]
  (println "Received an sqs event with message:" (get-in event [:Records 0 :body]))
  nil)

(h/deflambda ApiProxyMessage
  [{:keys [pathParameters] :as event} context]
  (let [message (or (:message pathParameters) "Hello")]
    (println (aws/invoke @sqs {:op :SendMessage
                               :request {:QueueUrl (-> context :envs :SQS_URL)
                                         :MessageBody message}}))
    {:statusCode 200
     :body (str "Received message: " message)}))

(h/gen-main [#'SubscribeLambda #'ApiProxyMessage])
