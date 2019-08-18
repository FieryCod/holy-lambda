(ns sqs-example.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h])
  (:import
   [com.amazonaws.services.sqs AmazonSQSClient]
   [com.amazonaws.services.sqs.model SendMessageRequest]))

(h/deflambda ReceiveStringLambda
  [event context]
  (h/info "Received an sqs event" event)
  (h/info context)
  (->> (SendMessageRequest. (-> context :envs :CONCATENATED_HOLY_SQS_URL) (str (get-in event [:Records 0 :body] "") " HolyLambda!"))
       (.sendMessage (AmazonSQSClient.)))
  (h/info "Successfully sent a message to ConcatenatedHolySQS")
  ;; Indicates that the message was succesfully processed,
  ;; therefore the message will be automatically removed from the
  ;; BaseStringSQS
  nil)

(h/gen-main [#'ReceiveStringLambda])
