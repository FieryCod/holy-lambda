(ns ^:no-doc ^:private fierycod.holy-lambda.impl.agent
  "Provides util `call-lambdas-with-agent-payloads` which helps GraalVM agent to generate the configuration
  necessary to successfully compile the project with `native-image`"
  (:require
   [fierycod.holy-lambda.impl.util :as u]
   [clojure.edn :as edn]
   [clojure.java.io :as io])
  (:import
   [java.nio.file Files LinkOption]
   [java.io File]))

(def ^:private PAYLOADS_PATH "resources/native-agents-payloads/")

(defn- agents-payloads->invoke-map
  []
  (->> (file-seq (io/file PAYLOADS_PATH))
       (filterv #(Files/isRegularFile (.toPath ^File %1) (into-array LinkOption [])))
       (mapv #(-> % slurp edn/read-string (assoc :path (str %))))
       (sort-by :path)))

(defn routes->reflective-call!
  [routes]
  (doseq [{:keys [name request path propagate]} (agents-payloads->invoke-map)]
    (println "[Holy Lambda] Calling lambda" name "with payloads from" path)
    (if propagate
      (u/call (routes name) request)
      (try
        (u/call (routes name) request)
        (catch Exception _err nil)))
    (println "[Holy Lambda] Succesfully called" name "with payloads from" path))
  (println "[Holy Lambda] Succesfully called all the lambdas"))
