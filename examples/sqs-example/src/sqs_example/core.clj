(ns sqs-example.core
  (:gen-class)
  (:require
   [fierycod.holy-lambda.core :as h])
  (:import
   [com.amazonaws.auth AWS4Signer]
   [com.amazonaws.services.sqs AmazonSQSClient]
   [com.amazonaws.services.sqs.model SendMessageRequest]))

(h/deflambda ReceiveStringLambda
  [event context]
  (h/info "Received an sqs event" event)
  (->> (SendMessageRequest. "Hello" (get (:envs context) "CONCATENATED_HOLY_SQS_URL"))
       (.sendMessage (AmazonSQSClient.))
       (println "Result of the message send: "))
    ;; (let [^SendMessageRequest request (-> (SendMessageRequest/builder)
    ;;                                       (.messageBody (str (get-in event [:Records 0 :body]) " HolyLambda!"))
    ;;                                       (.queueUrl (get (:envs context) "CONCATENATED_HOLY_SQS_URL"))
    ;;                                       (.build))]
    ;;   (.sendMessage (SqsClient/create) request)
  (h/info "Successfully sent a message to ConcatenatedHolySQS")
    ;; Indicates that the message was succesfully processed,
    ;; therefore the message will be automatically removed from the
    ;; BaseStringSQS
  nil)

(h/gen-main [#'ReceiveStringLambda])
