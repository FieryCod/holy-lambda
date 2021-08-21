(ns hl.aws-bucket.core
  (:gen-class)
  (:require
   ;; add this for graalvm (explicit load)
   ;; there are dynamically loaded at runtime
   [cognitect.aws.http.cognitect]
   [cognitect.aws.protocols.query]
   [cognitect.aws.protocols.json]
   [cognitect.aws.protocols.common]
   [cognitect.aws.protocols.rest]
   [cognitect.aws.protocols.rest-xml]
   [cognitect.aws.client.api :as aws]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.core :as h]))

;; (defn initialize-at-build-time-list
;;   [& args]
;;   (println
;;    (->> (map ns-name (all-ns))
;;         (remove #(clojure.string/starts-with? % "clojure"))
;;         (map #(clojure.string/split (str %) #"\."))
;;         (keep butlast)
;;         (map #(clojure.string/join "." %))
;;         distinct
;;         (map munge)
;;         (cons "clojure")
;;         (clojure.string/join ","))))

;; (initialize-at-build-time-list)
(def s3 (delay (aws/client {:api :s3})))

(defn ExampleLambda
  [{:keys [event ctx] :as request}]

  ;; return a successful plain text response. See also, hr/json
  (hr/json (boolean (:Buckets (aws/invoke @s3 {:op :ListBuckets})))))

(h/entrypoint [#'ExampleLambda])

(agent/in-context
 (aws/invoke @s3 {:op :ListBuckets}))
