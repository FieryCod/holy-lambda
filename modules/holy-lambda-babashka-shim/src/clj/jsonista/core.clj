(ns jsonista.core)

(def ^:private ERROR_MESSAGE "Artifact io.github.FieryCod/holy-lambda-babashka should not be used with other than babashka runtime to ensure minimal artifact size. If you're not decided which runtime to use then use io.github.FieryCod/holy-lambda which supports all runtimes. If you're looking for json library for babashka then just include cheshire.core.")

(defn read-value
  [& args]
  (throw (ex-info ERROR_MESSAGE {})))

(defn parse-string
  [& args]
  (throw (ex-info ERROR_MESSAGE {})))

(defn object-mapper
  [& args]
  (throw (ex-info ERROR_MESSAGE {})))

(defn write-value-as-bytes
  [& args]
  (throw (ex-info ERROR_MESSAGE {})))

(defn write-value-as-string
  [& args]
  (throw (ex-info ERROR_MESSAGE {})))
