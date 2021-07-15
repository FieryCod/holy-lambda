(ns aws-interop-v2.example.core
  (:gen-class)
  (:require [fierycod.holy-lambda.response :as hr]
            [fierycod.holy-lambda.core :as h]
            [fierycod.holy-lambda.agent :as agent]
            [fierycod.holy-lambda.native-runtime :as native]
            [clojure.string :as str]
            [jsonista.core :as json])
  (:import [java.io ByteArrayOutputStream ByteArrayInputStream]
           [java.time Instant]
           [java.net HttpURLConnection]
    ; AWS API
           [software.amazon.awssdk.regions Region]
           [com.amazonaws.xray.interceptors TracingInterceptor]
           [software.amazon.awssdk.core.client.config ClientOverrideConfiguration]
           [software.amazon.awssdk.auth.credentials EnvironmentVariableCredentialsProvider]
           [software.amazon.awssdk.http.urlconnection UrlConnectionHttpClient]
           [software.amazon.awssdk.core ResponseInputStream SdkBytes]
           [software.amazon.awssdk.http AbortableInputStream]
    ; AWS S3 API
    ; https://sdk.amazonaws.com/java/api/latest/index.html?software/amazon/awssdk/services/s3/package-summary.html
           [software.amazon.awssdk.services.s3 S3Client]
           [software.amazon.awssdk.services.s3.model S3Object
                                                     ListObjectsRequest ListObjectsResponse
                                                     GetObjectAclRequest GetObjectAclResponse]))

(def xray-enabled? true)

; cache of re-usable clients using the id/key. best perf to create a client once, not for each request
(def cloud-clients (atom {}))

(defn aws-client
  "return an aws client in order of priority:
   1. already present in request using the :id i.e. provided by tests etc
   2. the :conf client used during native:conf
   3. the :default used/re-used for cloud/prod operation"
  [{:keys [id default-thunk conf request]}]
  (or (get-in request [:event ::aws-clients id])
      (when (get-in request [:event ::native-conf?])
        conf)
      (or (get @cloud-clients id)
          (let [client (default-thunk)]
            (swap! cloud-clients assoc id client)
            client))))

(defn common-client-config
  [client {:keys [region x-ray?]}]
  (-> client
      (cond-> region (.region region)
              x-ray? (.overrideConfiguration (-> (ClientOverrideConfiguration/builder)
                                                 (.addExecutionInterceptor (TracingInterceptor.))
                                                 (.build))))
      (.credentialsProvider (EnvironmentVariableCredentialsProvider/create))
      (.httpClientBuilder (UrlConnectionHttpClient/builder))))

(defn s3-client
  [opts]
  (-> (S3Client/builder)
      (common-client-config opts)
      (.build)))

(defn s3-object
  [key size]
  (-> (S3Object/builder)
      (.key key)
      (.size size)
      (.build)))

(defn mock-s3-client
  [xfm]
  (reify S3Client
    (^ListObjectsResponse listObjects
      [^S3Client client ^ListObjectsRequest request]
      (-> (ListObjectsResponse/builder)
          (xfm :list)
          (.build)))
    (^GetObjectAclResponse getObjectAcl
      [^S3Client client ^GetObjectAclRequest request]
      (-> (GetObjectAclResponse/builder)
          (xfm :acl)
          (.build)))))

(defn s3-list-objects
  [client {:keys [bucket]}]
  (let [request (-> (ListObjectsRequest/builder)
                    (.bucket bucket)
                    (.build))]
    (.listObjects client request)))

(defn s3-get-acl
  [client {:keys [bucket key]}]
  (let [request (-> (GetObjectAclRequest/builder)
                    (.bucket bucket)
                    (.key key)
                    (.build))]
    (.getObjectAcl client request)))

(h/deflambda
  ExampleLambda
  "handles 2 uris: one to list an s3 bucket, the second to display meta-data for an s3 object"
  [{:keys [event ctx] :as request}]
  (let [{:keys [envs]} event
        bucket (get envs "S3_BUCKET")
        s3-client (aws-client {:id            :s3
                               :default-thunk (fn [] (s3-client {:x-ray? xray-enabled?}))
                               :conf          (mock-s3-client (fn [v _] v))
                               :request       request})]

    ; allow requests to control logging of full request. Note: this could record your AWS keys in cloudwatch
    (when (some-> event :queryStringParameters (str/includes? "LOG"))
      (println (json/write-value-as-string event))
      (println (json/write-value-as-string ctx)))

    (cond
      ; list page
      (= (:path request) "/list")
      (let [s3-list-response (s3-list-objects s3-client {:bucket bucket})]
        (hr/html (str "<h2>All Objects</h2>\n"
                      (->> (.contents s3-list-response)
                           (map (fn [obj]
                                  (str "<p><a href='/obj/" (.key obj) "'>"
                                       (.key obj) " : " (.size obj)
                                       "</a></p>")))
                           (str/join "\n")))))
      ; object perms page
      (str/starts-with? (:path request) "/obj")
      (let [s3-acl-response (s3-get-acl s3-client {:bucket bucket
                                                   :key    "TODO"})]
        (hr/html (str "<h2>"
                      (subs (:path request) 5 (count (:path request)))
                      "</h2>\n"
                      (->> (.grants s3-acl-response)
                           (map (fn [grant]
                                  (str "<p>" (.permissionAsString grant) "</p>")))
                           (str/join "\n"))))))))

(native/entrypoint [#'ExampleLambda])

(agent/in-context

  )
