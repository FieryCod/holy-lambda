(ns ^:no-doc ^:private fierycod.holy-lambda.agent
  "Provides utils for generating native-configurations via GraalVM agent.
   GraalVM agent is convenient tool for complex project, so that
   user does not have to put each entry of reflective call to configuration by hand."
  (:require
   [fierycod.holy-lambda.util :as u]
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
  (doseq [{:keys [request path propagate] :as invoke-map} (agents-payloads->invoke-map)]
    (println "[Holy Lambda] Calling lambda" (:name invoke-map) "with payloads from" path)
    (if propagate
      (u/call (routes (:name invoke-map)) request)
      (try
        (u/call (routes (:name invoke-map)) request)
        (catch Exception _err nil)))
    (println "[Holy Lambda] Succesfully called" (:name invoke-map) "with payloads from" path))
  (println "[Holy Lambda] Succesfully called all the lambdas"))
