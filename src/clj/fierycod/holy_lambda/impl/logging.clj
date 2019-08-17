(ns ^:no-doc ^:private fierycod.holy-lambda.impl.logging
  (:require
   [clojure.string :as string])
  (:import
   [com.amazonaws.services.lambda.runtime LambdaLogger]))

(defn- logger-factory
  [& [logger-impl]]
  (let [unified-logger (proxy [LambdaLogger] []
                         (log [s]
                           (println s)))]
    (or logger-impl unified-logger)))

(defn- decorate-log
  [severity vvs]
  (let [^String decor (case severity
                        :log ""
                        :info "[INFO] "
                        :warn "[WARN] "
                        :error "[ERROR] "
                        :fatal "[FATAL] "
                        :else "")
        ^String message (string/join " " vvs)]
    (str decor message)))

(def ^:dynamic ^LambdaLogger *logger*
  (logger-factory))

(defn log
  [& vs]
  (.log *logger* ^String (decorate-log :log vs)))

(defn info
  [& vs]
  (.log *logger* ^String (decorate-log :info vs)))

(defn warn
  [& vs]
  (.log *logger* ^String (decorate-log :warn vs)))

(defn error
  [& vs]
  (.log *logger* ^String (decorate-log :error vs)))

(defn fatal
  [& vs]
  (.log *logger* ^String (decorate-log :fatal vs)))

(defmacro trace
  [tag x]
  `(do (info ~tag ~x)
       ~x))
