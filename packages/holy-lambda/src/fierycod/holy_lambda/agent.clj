(ns ^:no-doc ^:private fierycod.holy-lambda.agent
  "Provides utils for generating native-configurations via GraalVM agent.
  GraalVM agent is convenient tool for complex project, so that
  user does not have to put each entry of reflective call to configuration by hand."
  (:require
   [fierycod.holy-lambda.util :as u]
   [clojure.edn :as edn]
   [clojure.string :as s]
   [clojure.java.io :as io])
  (:import
   [java.nio.file Files LinkOption]
   [java.io File]))

(def ^:private PAYLOADS_PATH "resources/native-agents-payloads/")
(def ^:private AGENT_EXECUTOR "native-agent")

(defmacro in-context
  "Executes body in safe agent context for native configuration generation.
  Useful when it's hard for agent payloads to cover all logic branches.

  *In order to generate native-configuration run:*

  ```
  make native-gen-conf
  ```

  *Usage:*

  ```

  (in-context
    (some-body-which-has-to-be-inspected-via-graalvm))
  ```

  You can safely leave agent-context calls in the code. Agent context not set results in no code being generated by macro.
  "
  [& body]
  (if-not (System/getenv "USE_AGENT_CONTEXT")
    nil
    `(when (= (System/getProperty "executor") @#'fierycod.holy-lambda.agent/AGENT_EXECUTOR)
       (try (do ~@body)
            (catch Exception err#
              (println "Exception in agent-context: " err#))))))

(defn- agents-payloads->invoke-map
  []
  (->> (file-seq (io/file PAYLOADS_PATH))
       (filterv #(Files/isRegularFile (.toPath ^File %1) (into-array LinkOption [])))
       (filterv #(s/includes? (str %) ".edn"))
       (mapv #(-> % slurp edn/read-string (assoc :path (str %))))
       (sort-by :path)))

(defn- routes->reflective-call!
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