(ns holy-lambda.tasks
  "This namespace contains tasks!"
  (:require
   [clojure.string :as s]
   [clojure.java.shell :as csh]
   [clojure.edn :as edn]
   [babashka.deps :as deps]
   [babashka.fs :as fs]
   [babashka.curl :as curl]
   [babashka.process :as p]
   [clojure.java.io :as io]))

(def TASKS_VERSION "0.0.1")
(def TASKS_VERSION_MATCH #"(?:TASKS_VERSION) (\"[0-9]*\.[0-9]*\.[0-9]*\")")

;; Taken from clojure-term-colors https://github.com/trhura/clojure-term-colors
(defn- escape-code
  [i]
  (str "\033[" i "m"))

(def ^:dynamic *colors*
  "foreground color map"
  (zipmap [:grey :red :green :yellow
           :blue :magenta :cyan :white]
          (map escape-code
               (range 30 38))))

(def ^:dynamic *highlights*
  "background color map"
  (zipmap [:on-grey :on-red :on-green :on-yellow
           :on-blue :on-magenta :on-cyan :on-white]
          (map escape-code
            (range 40 48))))

(def ^:dynamic *attributes*
  "attributes color map"
  (into {}
        (filter (comp not nil? key)
                (zipmap [:bold, :dark, nil, :underline,
                         :blink, nil, :reverse-color, :concealed]
                        (map escape-code (range 1 9))))))

(def ^:dynamic *reset* (escape-code 0))

;; Bind to true to have the colorize functions not apply coloring to
;; their arguments.
(def ^:dynamic *disable-colors* nil)

(defmacro define-color-function
  "define a function `fname' which wraps its arguments with
        corresponding `color' codes"
  [fname color]
  (let [fname (symbol (name fname))
        args (symbol 'args)]
    `(defn ~fname [& ~args]
       (if-not *disable-colors*
         (str (clojure.string/join (map #(str ~color %) ~args)) ~*reset*)
         (apply str ~args)))))

(defn define-color-functions-from-map
  "define functions from color maps."
  [colormap]
  (eval `(do ~@(map (fn [[color escape-code]]
                `(println ~color ~escape-code)
                `(define-color-function ~color ~escape-code))
                    colormap))))

(define-color-functions-from-map *colors*)
(define-color-functions-from-map *highlights*)
(define-color-functions-from-map *attributes*)

;; Taken from clojure-term-colors https://github.com/trhura/clojure-term-colors

;;;; helpers

(defn- exit-non-zero
  [proc]
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell [cmd & args]
  (exit-non-zero (p/process (into (p/tokenize cmd) args) {:inherit true})))

(defn- clojure [cmd & args]
  (exit-non-zero (deps/clojure (into (p/tokenize cmd) args))))

(defn shs
  [cmd & args]
  (p/process (into (p/tokenize cmd) args) {:inherit true}))

(defn command-exists?
  [cmd]
  (= (int (:exit (csh/sh "which" cmd))) 0))

(defn accent
  [s]
  (underline (blue s)))

(defn hpr
  [& args]
  (apply println (accent "[holy-lambda]") args))

(defn pre
  [s]
  (red s))

(defn prw
  [s]
  (yellow s))

(defn prs
  [s]
  (green s))

(defn plist
  [xs]
  (s/join "" (mapv (fn [x]
                     (str " - " x "\n"))
                   xs)))

(defn modify-lambda-options
  [modify-fn]
  (if-let [opts (:holy-lambda/options (edn/read-string (slurp (io/file "bb.edn"))))]
    (spit "bb.edn" (modify-fn opts))
    (hpr (pre "Either bb.edn not found or does not contain :holy-lambda/options"))))

(defn doctor
  "Diagnoses common issues of holy-lambda stack"
  []
  (hpr (accent "Checking health of holy-lambda stack"))
  (if-let [cmds-not-found (seq (filter (comp not command-exists?) ["aws" "sam" "bb" "docker" "clojure" "zip"]))]
    (hpr (str (pre (str "Commands " cmds-not-found " not found. Install all then run: ")) (underline "bb doctor")))
    (hpr (prs (str "Looks cool to me")))))


(defn version
  []
  (hpr (prs "Current tasks version is:" (accent TASKS_VERSION))))

(defn sync:update
  "Syncs or update holy-lambda remote tasks bb.edn into local."
  []
  (let [local-tasks ".holy-lambda/script/holy_lambda/tasks.clj"
        tasks-remote (:body (curl/get "https://github.com/FieryCod/holy-lambda/tree/master/modules/holy-lambda-babashka-tasks/script/holy_lambda/tasks.clj"))]
    (hpr (accent "Syncing with remote holy-lambda tasks"))
    (if-not (fs/exists? (io/file local-tasks))
      (do
        (hpr (prw "Remote tasks not synced with local. Syncing.."))
        (spit local-tasks tasks-remote)
        (hpr (prs "Remote tasks synced with local!")))
      (do
        (hpr "Local tasks exists. Trying to update!")
        (hpr "Local version: "
             TASKS_VERSION
             ", remote version: "


             )
      )


    )
    ))

;; (fs/exists? (io/file ".holy-lambda/script/holy_lambda/tasks.clj"))

(defn purge
  "Deletes target artifacts"
  []
  (let [artifacts ["packaged.yml" "target" "packaged-native-yml" "output" "latest.zip" "BABASHKA_ENTRYPOINT" "bootstrap" "Dockerfile.ee"]]
    (hpr  (str (accent "Cleaning artifacts:") "\n\n" (plist artifacts)))

    (doseq [art artifacts]
      (shell (str "rm -rf " art)))
    (hpr  (prs "Artifacts cleaned"))))

(defn docker-build-ee
  []
  (hpr  (accent "Building GraalVM EE docker image"))
  (spit "Dockerfile.ee" (:body (curl/get "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/docker/ee/Dockerfile")))
  (shell "docker build . -f Dockerfile.ee -t fierycod/graalvm-native-image:ee")
  (shell "rm -rf Dockerfile.ee"))

;;;; end helpers

;;;; tasks

(defn repl
  "sdasd"
  []
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
      (hpr  "Not deploying because CLOJARS_USERNAME isn't set."))
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
