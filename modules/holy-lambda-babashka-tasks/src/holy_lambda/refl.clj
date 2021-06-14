(ns holy-lambda.refl
  {:author "Michiel Borkent"
   :doc "Code adapted for purpose of HL from refl project: https://github.com/borkdude/refl/blob/main/script/gen-reflect-config.clj.
         All credits goes to @borkdude"}
  (:require
   [babashka.process :refer [process]]
   [cheshire.core :as cheshire]
   [clojure.string :as str]
   [clojure.string :as s]))

;; (def trace-json (cheshire/parse-string (slurp "trace-file.json") true))

;; [Z = boolean
;; [B = byte
;; [S = short
;; [I = int
;; [J = long
;; [F = float
;; [D = double
;; [C = char
;; [L = any non-primitives(Object)

(defn normalize-array-name
  [n]
  ({"[F" "float[]"
    "[B" "byte[]"
    "[Z" "boolean[]"
    "[C" "char[]"
    "[D" "double[]"
    "[I" "int[]"
    "[J" "long[]"
    "[S" "short[]"}
   n
   n))

(def ignored (atom #{}))
(def unignored (atom #{}))

(defn ignore
  [{:keys [tracer caller_class function args] :as _m}]
  (when (= "reflect" tracer)
    (when-let [arg (first args)]
      (let [arg (normalize-array-name arg)]
        (if (and caller_class
                 (or
                  (and (= function "forName")
                       (= class "java.lang.Class"))
                  (= "clojure.lang.RT" caller_class)
                  (= "clojure.genclass__init" caller_class)
                  (and (str/starts-with? caller_class "clojure.core$fn")
                       (= "java.sql.Timestamp" arg)))
                 (= "forName" function))
          (swap! ignored conj arg)
          (when (= "clojure.lang.RT" caller_class)
            ;; unignore other reflective calls in clojure.lang.RT
            (swap! unignored conj arg)))))))

(defn process-1
  [{:keys [:name] :as m}]
  (when-not (and (= 1 (count m))
                 (contains? @ignored name)
                 (not (contains? @unignored name)))
    ;; fix bug(?) in automated generated config
    (if (= "java.lang.reflect.Method" name)
      (assoc m :name "java.lang.reflect.AccessibleObject")
      m)))

;; Tidy up processing of resources and reflections
(defn clean-reflection-config!
  []
  (let [tracing-config (cheshire/parse-string (slurp "resources/native-configuration/traces.json") true)
        _! (run! ignore tracing-config)
        reflection-config (cheshire/parse-string (slurp "resources/native-configuration/reflect-config.json") true)
        cleaned-reflection-config (keep process-1 reflection-config)]

    (spit "resources/native-configuration/reflect-config.json" (cheshire/generate-string cleaned-reflection-config {:pretty true}))
    (spit "resources/native-configuration/reflect-config.orig.json" (cheshire/generate-string reflection-config {:pretty true}))))
