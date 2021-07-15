(ns aws-interop-v2.example.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.pprint :refer [pprint]]
            [clj-fakes.core :as f]
            [aws-interop-v2.example.core :as handler]
            [clojure.string :as str])
  (:import (software.amazon.awssdk.services.s3.model ListObjectsRequest ListObjectsResponse GetObjectAclResponse GetObjectAclRequest Grant)))

(deftest list-objects
  (f/with-fakes
    (let [fake-response (-> (ListObjectsResponse/builder)
                            (.contents [(handler/s3-object "foo" 100)
                                        (handler/s3-object "bar" 200)])
                            (.build))
          s3-client (f/reify-fake software.amazon.awssdk.services.s3.S3Client
                                  (listObjects :recorded-fake [f/any (constantly fake-response)]))
          request-from-api-gateway {:path  "/list"
                                    :event {::handler/aws-clients {:s3 s3-client}
                                            :envs                 {"S3_BUCKET" "fakebucketname"}}
                                    :ctx   {}}
          response-for-api-gateway (handler/ExampleLambda request-from-api-gateway)]
      (is (str/includes? (:body response-for-api-gateway) "foo : 100")
          "obj 1 in list")
      (is (str/includes? (:body response-for-api-gateway) "bar : 200")
          "obj 1 in list")
      (is (f/method-was-called-once "listObjects" s3-client
                                    [(f/arg (fn [v]
                                              (is (= "fakebucketname" (.bucket v))
                                                  "list API call was passed bucket name from environment")
                                              (instance? ListObjectsRequest v)))])
          "handler called AWS to get list of objects in the bucket")
      (is (= 200 (:statusCode response-for-api-gateway))))))

(deftest object-acl
  (f/with-fakes
    (let [fake-response (-> (GetObjectAclResponse/builder)
                            (.grants [(-> (Grant/builder)
                                          (.permission "write")
                                          (.build))])
                            (.build))
          s3-client (f/reify-fake software.amazon.awssdk.services.s3.S3Client
                                  (getObjectAcl :recorded-fake [f/any (constantly fake-response)]))
          request-from-api-gateway {:path  "/obj/envs.json"
                                    :event {::handler/aws-clients {:s3 s3-client}
                                            :envs                 {"S3_BUCKET" "fakebucketname"}}
                                    :ctx   {}}
          response-for-api-gateway (handler/ExampleLambda request-from-api-gateway)]
      (is (str/includes? (:body response-for-api-gateway) "write")
          "1 fake perm in list")
      (is (f/method-was-called-once "getObjectAcl" s3-client
                                    [(f/arg (fn [v]
                                              (is (= "fakebucketname" (.bucket v))
                                                  "list API call was passed bucket name from environment")
                                              (instance? GetObjectAclRequest v)))])
          "handler called AWS to get ACL for the object")
      (is (= 200 (:statusCode response-for-api-gateway))))))



