(ns holy-lambda.refl
  {:author "Michiel Borkent"
   :doc "Code adapted for purpose of HL from refl project: https://github.com/borkdude/refl/blob/main/script/gen-reflect-config.clj.
         All credits goes to @borkdude"}
  (:require
   [cheshire.core :as cheshire]
   [clojure.string :as str]))

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

(def ignored-by-arg (atom #{}))
(def ignored-by-class (atom #{}))
(def unignored-by-arg (atom #{}))

(defn ignore-by-class!
  [{:keys [tracer caller_class class] :as _m}]
  (when (= "reflect" tracer)
    (when (contains? #{"clojure.lang.Compiler$StaticFieldExpr"
                       "clojure.lang.Compiler$ObjExpr"
                       "clojure.lang.Compiler$NewInstanceExpr"}
                     caller_class)
      (swap! ignored-by-class conj class))))

(defn ignore-by-arg!
  [{:keys [tracer caller_class function args] :as _m}]
  (when (= "reflect" tracer)
    (when-let [arg (first args)]
      (let [arg (normalize-array-name arg)]
        (if (and caller_class
                 (or
                  (and )
                  (= "clojure.lang.RT" caller_class)
                  (= "clojure.genclass__init" caller_class)
                  (and (str/starts-with? caller_class "clojure.core$fn")
                       (= "java.sql.Timestamp" arg)))
                 (= "forName" function))
          (swap! ignored-by-arg conj arg)
          (when (= "clojure.lang.RT" caller_class)
            ;; unignore other reflective calls in clojure.lang.RT
            (swap! unignored-by-arg conj arg)))))))


(defn keep-by-arg!
  [{:keys [name] :as m}]
  (when-not (and (= 1 (count m))
                 (contains? @ignored-by-arg name)
                 (not (contains? @unignored-by-arg name)))
    ;; fix bug(?) in automated generated config
    (if (= "java.lang.reflect.Method" name)
      (assoc m :name "java.lang.reflect.AccessibleObject")
      m)))

(defn keep-by-class!
  [{:keys [name] :as m}]
  (when-not (contains? @ignored-by-class name)
    m))

(def keep!
  (comp keep-by-arg! keep-by-class!))

(defn ignore!
  [m]
  (doseq [afn [ignore-by-arg! ignore-by-class!]]
    (afn m)))

;; Tidy up processing of resources and reflections
(defn clean-reflection-config!
  []
  (let [tracing-config (cheshire/parse-string (slurp "resources/native-configuration/traces.json") true)
        _! (run! ignore! tracing-config)
        reflection-config (cheshire/parse-string (slurp "resources/native-configuration/reflect-config.json") true)
        cleaned-reflection-config (keep keep! reflection-config)]

    (spit "resources/native-configuration/reflect-config.json" (cheshire/generate-string cleaned-reflection-config {:pretty true}))
    (spit "resources/native-configuration/reflect-config.orig.json" (cheshire/generate-string reflection-config {:pretty true}))))

(comment
  (def traces1 (cheshire/parse-string (slurp "./src/holy_lambda/traces1.json") true))
  (def reflects1 (cheshire/parse-string (slurp "./src/holy_lambda/reflect1.json") true))

  (do
    (reset! ignored-by-arg #{})
    (reset! ignored-by-class #{})
    )

  (run! ignore! traces1)
  @ignored-by-arg
  @ignored-by-class

  (keep keep! reflects1)

  )
