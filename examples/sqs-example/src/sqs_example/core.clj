(ns sqs-example.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h])
  (:import
   [org.apache.commons.logging.impl LogFactoryImpl]
   [com.amazonaws.services.sqs.model SendMessageRequest]
   [com.amazonaws.services.sqs AmazonSQSClient]))

(h/deflambda ReceiveStringLambda
  [event context]
  (h/info "Received an sqs event" event)
  (-> (AmazonSQSClient.)
      (.sendMessage (SendMessageRequest. (get (:envs context) "CONCATENATED_HOLY_SQS_URL")
                                         (str (get-in event [:Records 0 :body]) " HolyLambda!"))))
  (h/info "Successfully sent a message to ConcatenatedHolySQS")
  ;; Indicates that the message was succesfully processed,
  ;; therefore the message will be automatically removed from the
  ;; BaseStringSQS
  nil)

(h/gen-main [#'ReceiveStringLambda])
