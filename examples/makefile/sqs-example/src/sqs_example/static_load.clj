(ns sqs-example.static-load
  (:require
   [cognitect.aws.client.api]
   ;; add this for graalvm (explicit load)
   ;; there are dynamically loaded at runtime
   [cognitect.aws.http.cognitect]
   [cognitect.aws.protocols.query]
   [cognitect.aws.protocols.json]
   [cognitect.aws.protocols.common]
   [cognitect.aws.protocols.rest]
   [cognitect.aws.protocols.rest-xml]))

