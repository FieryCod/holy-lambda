(ns fierycod.holy-lambda.impl.agent
  "Provides util `call-lambdas-with-agent-payloads` which helps GraalVM agent to generate the configuration
  necessary to successfully compile the project with `native-image`"
  (:require
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
       (sort-by :name)))

(defn call-lambdas-with-agent-payloads
  [caller log-fn]
  (doseq [{:keys [name event context path]} (native-agents-files->payloads-map)]
    (log-fn "Calling lambda" name "with payloads from" path)
    ))

(call-lambdas-with-agent-payloads fierycod.holy-lambda.core/call fierycod.holy-lambda.core/info)
