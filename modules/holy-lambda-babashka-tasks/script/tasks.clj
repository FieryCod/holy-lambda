(ns tasks
  "This namespace contains tasks!"
  (:require [babashka.deps :as deps]
            [babashka.fs :as fs]
            [babashka.process :as p]))

;;;; helpers

(defn- exit-non-zero [proc]
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell [cmd & args]
  (exit-non-zero (p/process (into (p/tokenize cmd) args) {:inherit true})))

(defn- clojure [cmd & args]
  (exit-non-zero (deps/clojure (into (p/tokenize cmd) args))))

;;;; end helpers

;;;; tasks

(defn repl []
  (shell "iced repl -A:dev"))

(defn outdated []
  (clojure "-M:outdated:nop --update"))

(defn tests []
  (clojure "-M:outdated:nop:1.8 --exclude=clojure/brew-install")
  (clojure "clojure -M:dev:1.9:test")
  (clojure "clojure -M:dev:test"))

(defn lint []
  (shell "echo cljstyle") ;; I don't have clj-style
  (shell "clj-kondo --lint src:test"))

(defn pom []
  (clojure "-Spom"))

(defn clean []
  (shell "rm -rf .cpcache target"))

(def standalone-jar-file "target/antq-standalone.jar")

(defn uberjar []
  (pom)
  (clean)
  (clojure "-X:depstar uberjar
           :aot true
           :main-class antq.core
           :aliases [:nop]
           :jar" standalone-jar-file))

(def jar-file "target/antq.jar")

(defn jar []
  (pom)
  (clean)
  (clojure "-X:depstar jar :jar" jar-file))

(defn install []
  (clean)
  (jar)
  (clojure "-X:deploy :installer :local :artifact" jar-file))

(defn deploy []
  (if-not (System/getenv "CLOJARS_USERNAME")
    (binding [*out* *err*]
      (println "Not deploying because CLOJARS_USERNAME isn't set."))
    (do
      (clean)
      (jar)
      (clojure "-X:deploy :installer :remote :artifact" jar-file))))

(defn docker []
  (shell "docker build -t uochan/antq ."))

(defn docker-test []
  (shell "docker run --rm -v"
         (str (fs/absolutize "") ":/src")
         "-w" "/src" "uochan/antq:latest"))

(defn coverage
  "Run test coverage."
  []
  (clojure "-M:coverage:dev:nop --src-ns-path=src --test-ns-path=test --codecov"))
