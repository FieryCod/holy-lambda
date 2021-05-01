(ns sqs.example.core
  (:gen-class)
  (:require
   [sqs.example.static-load]
   [cognitect.aws.http.cognitect :as http]
   [cognitect.aws.client.api :as aws]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

(def http-client (delay (http/create)))
(def sqs (delay (aws/client {:api :sqs
                             :http-client @http-client})))

(defn send-sqs-message!
  [{:keys [queue-url msg]}]
  (aws/invoke @sqs {:op :SendMessage
                    :request {:QueueUrl queue-url
                              :MessageBody msg}}))
(h/deflambda SubscribeLambda
  [{:keys [event]}]
  (println "Received an sqs event with message:" (get-in event [:Records 0 :body]))
  ;; Ack message :)
  nil)

(h/deflambda ApiProxyMessage
  [{:keys [event ctx] :as _request}]
  (let [message (or (:message (:pathParameters event)) "Hello")]
    (println (send-sqs-message! {:queue-url (get-in ctx [:envs "SQS_URL"])
                                 :msg message}))
    (hr/text (str "Received message: " message))))

(native/entrypoint [#'SubscribeLambda #'ApiProxyMessage])

(agent/in-context
 (println "I will help in generation of native-configurations"))

(agent/in-context
 (send-sqs-message! {:msg ""}))
