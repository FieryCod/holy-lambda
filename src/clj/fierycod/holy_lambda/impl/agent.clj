(ns ^:no-doc ^:private fierycod.holy-lambda.impl.agent
  "Provides util `call-lambdas-with-agent-payloads` which helps GraalVM agent to generate the configuration
  necessary to successfully compile the project with `native-image`"
  (:require
   [fierycod.holy-lambda.impl.logging :as l]
   [fierycod.holy-lambda.impl.util :as u]
   [clojure.data.json :as json]
   [clojure.java.io :as io])
  (:import
   [java.nio.file Files LinkOption]
   [java.io File]))

(defn- native-agents-files->payloads-map
  []
  (->> (io/file "resources/native-agents-payloads/")
       file-seq
       (filter #(Files/isRegularFile (.toPath ^File %1) (into-array LinkOption [])))
       (map #(-> % slurp json/read-json (assoc :path (str %))))
       (sort-by :path)))

(defn call-lambdas-with-agent-payloads
  [routes]
  (doseq [{:keys [name event context path propagate]} (native-agents-files->payloads-map)]
    (l/info "Calling lambda" name "with payloads from" path)
    (if propagate
      (u/call (routes name) event context)
      (try
        (u/call (routes name) event context)
        (catch Exception err
          nil)))
    (l/info "Succesfully called" name "with payloads from" path))
  (l/info "Succesfully called all the lambdas"))
