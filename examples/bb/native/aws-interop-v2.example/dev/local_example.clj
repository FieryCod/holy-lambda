(ns local-example
  (:require [aws-interop-v2.example.core :as handler]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [ring.util.response :as response])
  (:import (software.amazon.awssdk.services.s3.model GetObjectAclResponse Grant)))

(defn api-gateway-transform
  "emulate how AWS API Gateway transforms proxy lambda responses to an http response but for a local ring server"
  [lambda-response]
  (-> lambda-response
      (set/rename-keys {:statusCode :status})))

(defn response-decorator
  "add sample S3 data to an AWS API response builder using the local filesystem as the source"
  [response-builder k request]
  (let [root-dir (->> (io/file "")
                      (.getAbsolutePath)
                      io/file)]
    (case k
      :list (.contents response-builder (->> root-dir
                                             (.listFiles)
                                             (mapv (fn [f]
                                                     (handler/s3-object (.getName f) (.length f))))))
      :acl (let [file-name (get-in request [:path-params :key])
                 file (io/file root-dir file-name)
                 fake-grants (->> [(when (.exists file) "exists")
                                   (when (.canRead file) "writable")
                                   (when (.canWrite file) "readable")]
                                  (keep (fn [grant]
                                          (when grant
                                            (-> (Grant/builder)
                                                (.permission grant)
                                                (.build))))))]
             (.grants response-builder fake-grants)))))

(defn example-handler
  [{:keys [headers path-params uri] :as request}]
  (let [handler-reponse (-> {:event {:headers        headers
                                     :httpMethod     "GET"
                                     :pathParameters {:proxy (->> [:org-id :version-id]
                                                                  (map path-params)
                                                                  (str/join "/"))}}
                             :ctx   {}
                             :path  uri}
                            (assoc-in [:event ::handler/aws-clients :s3]
                                      (handler/mock-s3-client (fn [builder k]
                                                                (response-decorator builder k request))))
                            handler/ExampleLambda)]
    (if (= 200 (:statusCode handler-reponse))
      (api-gateway-transform handler-reponse)
      {:status 500
       :body   "native handler failed"})))

(defn handle-exception
  [e]
  (.printStackTrace e)
  {:status 500
   :body   "Exception in local server logs"})

; indirection for dev changes without restart
(defn example-handler' [r] (try (example-handler r) (catch Exception e (handle-exception e))))

(defn start [_] (response/redirect "/list"))

(def routes [["/" {:get start}]
             ["/list" {:get example-handler'}]
             ["/obj/:key" {:get example-handler'}]])

