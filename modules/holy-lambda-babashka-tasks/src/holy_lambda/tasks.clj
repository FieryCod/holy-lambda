(ns holy-lambda.tasks
  "holy-lambda cmd application"
  (:require
   [clojure.string :as s]
   [clojure.pprint :as pprint]
   [clojure.java.shell :as csh]
   [clojure.edn :as edn]
   [cheshire.core :as json]
   [babashka.deps :as deps]
   [babashka.fs :as fs]
   [babashka.curl :as curl]
   [babashka.process :as p]
   [clojure.java.io :as io]
   [holy-lambda.refl :as refl])
  (:refer-clojure :exclude [spit]))

(def TASKS_VERSION "0.2.3")

(deps/add-deps {:deps {'clojure-term-colors/clojure-term-colors {:mvn/version "0.1.0"}}})

(defn env-true?
  [env]
  (when-let [prop (System/getenv env)]
    (contains? #{"true" "1"} prop)))

(def HL_DEBUG? (env-true? "HL_DEBUG"))

(require
 '[clojure.term.colors :refer [underline blue yellow red green]])

;;;; [START] HELPERS

(defn normalize-file-path
  [path]
  (s/replace (if (string? path)
               path
               (str path)) #"~/" (str (.getAbsolutePath (io/file (System/getProperty "user.home"))) "/")))

(defn string->keyword
  [s]
  (keyword (s/replace (str s) #":" "")))

(defn file-exists?
  [file]
  (boolean (some-> file normalize-file-path fs/exists?)))

(defn norm-args
  [args]
  (into {}
        (mapv
            (fn [[k v]]
              [(cond-> k
                 (s/includes? k "--")
                 (subs 2)

                 (s/includes? k ":")
                 (subs 1)

                 true
                 keyword)
               (or v true)])
            (partition-all 2 args))))

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

(defn hprd
  [& args]
  (apply println (accent "[holy-lambda][debug]") args))

(defn- exit-non-zero
  [proc]
  (when HL_DEBUG? (hprd (s/trim (s/join " " (:cmd @proc)))))
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell
  [cmd & args]
  (exit-non-zero (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit true})))

(defn- shell-no-exit
  [cmd & args]
  (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit true}))

(defn shsp
  [cmd & args]
  (let [result (apply csh/sh (remove nil? (into (p/tokenize cmd) args)))]
    (if (s/blank? (:err result))
      (when-not (s/blank? (:out result))
        (println (prs (:out result))))
      (println (pre (:err result))))))

(defn shs
  [cmd & args]
  (let [result (apply csh/sh (remove nil? (into (p/tokenize cmd) args)))]
    (if (s/blank? (:err result))
      (when-not (s/blank? (:out result))
        (:out result))
      (:err result))))

(defn shs-no-err
  [cmd & args]
  (let [result (apply csh/sh (remove nil? (into (p/tokenize cmd) args)))]
    (if (s/blank? (:err result))
      (when-not (s/blank? (:out result))
        (:out result))
      nil)))

(defn command-exists?
  [cmd]
  (= (int (:exit (csh/sh "which" cmd))) 0))

(defn plist
  [xs]
  (s/join "" (mapv (fn [x]
                     (str " - " x "\n"))
                   xs)))
;;;; [END] HELPERS

(def OS (let [os (s/lower-case (System/getProperty "os.name"))]
          (cond
            (s/includes? os "nux") :unix
            (s/includes? os "mac") :mac
            (s/includes? os "win") :windows
            :else :unknown)))

(when (contains? #{:unknown :windows} OS)
  (hpr (pre (str "OS: " OS " is not supported by holy-lambda. Please make an issue on Github!")))
  (System/exit 1))

(defn spit
  [file content]
  (io/make-parents file)
  (clojure.core/spit file content))

(def REMOTE_TASKS "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/modules/holy-lambda-babashka-tasks/STABLE_VERSION")

(defn bb-edn
  []
  (edn/read-string (slurp (io/file "bb.edn"))))

(def BB_EDN
  (try
    (bb-edn)
    (catch Exception err
      (do
        (hpr (pre "File") (accent "bb.edn") (pre "not found?!")
             (str "\n" (pre "Original message: ") (pre (.getMessage err))))
        (System/exit 1)))))

(def OPTIONS
  (if-let [opts (:holy-lambda/options BB_EDN)]
    opts
    (do (hpr (accent ":holy-lambda/options") (pre "are not declared in") (accent "bb.edn") (pre "file!")
             (pre "Exiting!"))
        (System/exit 1))))

(def TTY? (= (:exit @(shell-no-exit "test" "-t" "1")) 0))
(def DOCKER (:docker OPTIONS))
(def BUILD (:build OPTIONS))
(def HL_NO_DOCKER?
  (boolean (or (nil? DOCKER)
               (env-true? "HL_NO_DOCKER"))))

(let [home (and HL_NO_DOCKER?
                (some-> (or (System/getenv "GRAALVM_HOME")
                            (:graalvm-home BUILD))
                        (str "/bin/")))
      java (some-> home (str "java") normalize-file-path)
      native-image (some-> home (str "native-image") normalize-file-path)
      fallback-command (fn [command fallback]
                         (if (file-exists? command)
                           command
                           (do
                             (hpr (prw "Executable") (accent (or command "UNKNOWN")) (prw "does not exists! Using fallback:") (accent fallback) (prw "instead! Did you set up GRAALVM_HOME?"))
                             fallback)))]
  (def JAVA_COMMAND
    (if-not HL_NO_DOCKER?
      "java"
      (fallback-command java "java")))

  (def NATIVE_IMAGE_COMMAND
    (if-not HL_NO_DOCKER?
      "native-image"
      (fallback-command native-image "native-image"))))

(when-not (or HL_NO_DOCKER?
              (and (= OS :unix)
                   (= (:exit (csh/sh "pgrep" "-f" "docker")) 0))
              (and (= OS :mac)
                   (= (:exit (csh/sh "pgrep" "-f" "Docker.app")) 0)))
  (hpr (pre "Docker is not running! Enable and run docker first before using holy-lambda!"))
  (System/exit 1))

(def CLJ_ALIAS_KEY (or (some-> (System/getenv "HL_CLJ_ALIAS") string->keyword)
                       (some-> (:clj-alias BUILD) string->keyword)))

(when (and CLJ_ALIAS_KEY (not (keyword? CLJ_ALIAS_KEY)))
  (hpr (pre "Defined") (accent "build:clj-alias") (pre "should be a keyword")))

(when (and CLJ_ALIAS_KEY (not (CLJ_ALIAS_KEY (:aliases (read-string (slurp "deps.edn"))))))
  (hpr (pre "Defined")
       (accent "build:clj-alias")
       (accent CLJ_ALIAS_KEY)
       (pre "does not exists in") (accent "deps.edn")))

(def CLJ_ALIAS (some-> CLJ_ALIAS_KEY name))

(defn stat-file
  [filename]
  (when-not (file-exists? filename)
    (hpr "PATH" (accent filename) "does not exists.. Exiting!")
    (System/exit 1)))

(defn print-task
  [task-name]
  (hpr (str "Command " (red "<") (accent task-name) (red ">"))))

(def IMAGE_CORDS
  (or
   (System/getenv "HL_DOCKER_IMAGE")
   (:image DOCKER)
   "fierycod/graalvm-native-image:ce"))

(defn docker-image-exists?
  [image]
  (boolean (shs-no-err "docker" "inspect" "--type=image" image)))

(when-not (docker-image-exists? IMAGE_CORDS)
  (hpr (prw "Docker image") (accent IMAGE_CORDS) (prw "for") (accent "holy-lambda") (prw "microframework has not been yet downloaded!"))
  (hpr "Pulling the image" (accent IMAGE_CORDS) "from" (str (accent "DockerHub") "!"))
  (shell "docker" "pull" IMAGE_CORDS)
  (println ""))

(def DOCKER_VOLUMES_CONF (:volumes DOCKER))
(def DOCKER_VOLUMES
  (if-not (some-> DOCKER_VOLUMES_CONF seq)
    []
    (mapv (fn [{:keys [docker host]}]
            (when (or (not docker) (not host))
              (hpr (pre "Both") (accent ":docker") (pre "and") (accent ":host") (pre "properties should exist in") (str (accent "docker-volumes") (pre "!")))
              (System/exit 1))

            (when-not (file-exists? host)
              (hpr (pre "Host path:") (accent host) (pre "from") (accent ":docker-volumes") (pre "does not exists!"))
              (System/exit 1))

            (when (s/starts-with? docker "/project")
              (hpr (pre "Docker path:") (accent docker) (pre "from") (accent ":docker-volumes") (pre "cannot be mounted in /project or /project/**."))
              (System/exit 1))

            (when (= docker "/")
              (hpr (pre "Docker path:") (accent docker) (pre "from") (accent ":docker-volumes") (pre "cannot be mounted in / path."))
              (System/exit 1))

            (str (fs/absolutize (io/file host)) ":" docker))
          DOCKER_VOLUMES_CONF)))

(def DOCKER_NETWORK (:network DOCKER))
(def RUNTIME (:runtime OPTIONS))
(def NATIVE_IMAGE_ARGS (if-let [args (seq (:native-image-args RUNTIME))]
                         (s/join " " args)
                         nil))

(def HL_NO_PROFILE? (env-true? "HL_NO_PROFILE"))
(def AWS_PROFILE
  (when-not HL_NO_PROFILE?
    (or (System/getenv "HL_PROFILE")
        (System/getenv "AWS_PROFILE")
        (System/getenv "AWS_DEFAULT_PROFILE")
        "default")))

(when (and (not AWS_PROFILE) (not HL_NO_PROFILE?))
  (hpr (prw "No AWS Profile has been specified. ACCESS, SECRET keys from environment variables will be taken instead!")
       (prw "You can use env variable HL_NO_PROFILE=1 to hide this message!")))

(defn obtain-from-aws-profile
  [what]
  (let [result (csh/sh "aws" "configure" "get" what "--profile" AWS_PROFILE)]
    (if-not (= (:exit result) 0)
      (throw (ex-info (str (pre "AWS configuration check failed. Unable to get value from the profile: ") (accent AWS_PROFILE)
                           (when-not (s/blank? (:err result))
                             (str "\n" (pre (:err result))))
                           (pre "\nDid you run command: ") (accent "aws configure") (pre "?"))
                      {}))
      (s/trim (:out result)))))

(def REGION
  (or (System/getenv "HL_REGION")
      (System/getenv "AWS_REGION")
      (System/getenv "AWS_DEFAULT_REGION")
      (if HL_NO_PROFILE?
        (do
          (hpr (pre "Unable to get region from any of the sources: envs, credentials file."))
          (System/exit 1))
        (try
          (obtain-from-aws-profile "region")
          (catch Exception e
            (hpr (ex-message e))
            (System/exit 1))))))

(def ENTRYPOINT (:entrypoint (:runtime OPTIONS)))
(def OUTPUT_JAR_PATH ".holy-lambda/build/output.jar")
(def OUTPUT_JAR_PATH_WITH_AGENT ".holy-lambda/build/output-agent.jar")
(def HOLY_LAMBDA_DEPS_PATH ".holy-lambda/clojure/deps.edn")
(def REQUIRED_COMMANDS ["aws" "sam" "bb" "docker" "clojure" "zip" "id" "bash"])
(def NATIVE_CONFIGURATIONS_PATH "resources/native-configuration")
(def NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH "resources/native-configuration/resource-config.json")
(def BOOTSTRAP_FILE (:bootstrap-file RUNTIME))
(def NATIVE_DEPS_PATH (:native-deps RUNTIME))

(def AWS_ACCESS_KEY_ID (atom (System/getenv "AWS_ACCESS_KEY_ID")))
(def AWS_SECRET_ACCESS_KEY (atom (System/getenv "AWS_SECRET_ACCESS_KEY")))

(when (and (not HL_NO_PROFILE?)
           (not @AWS_ACCESS_KEY_ID)
           (not @AWS_SECRET_ACCESS_KEY))
  (if-not (command-exists? "aws")
    (do (hpr (accent "aws") (pre "command does not exists. Did you install AWS command line application?"))
        (System/exit 1))
    (try
      (let [access-key (obtain-from-aws-profile "aws_access_key_id")
            secret-key (obtain-from-aws-profile "aws_secret_access_key")]
        (reset! AWS_ACCESS_KEY_ID access-key)
        (reset! AWS_SECRET_ACCESS_KEY secret-key))
      (catch Exception e
        (hpr (ex-message e))
        (System/exit 1)))))

(defn exit-if-not-synced!
  []
  (when (and (not HL_NO_DOCKER?)
             (not (file-exists? ".holy-lambda/clojure/deps.edn")))
    (hpr (pre "Project has not been synced yet. Run") (accent "stack:sync") (pre "before running this command!"))
    (System/exit 1)))

(def USER_GID
  (str (s/trim (shs "id -u"))
       ":" (s/trim (shs "id -g"))))

(def HOME_DIR
  (.getAbsolutePath
   (io/file (or
             (System/getenv "XDG_CACHE_HOME")
             (System/getProperty "user.home")))))

(def PROJECT_DIRECTORY
  (.getAbsolutePath (io/file (System/getProperty "user.dir"))))

(def AWS_DIR
  (.getAbsolutePath (io/file HOME_DIR ".aws")))

(def AWS_DIR_EXISTS?
  (file-exists? AWS_DIR))

(defn edn->pp-sedn
  [edn]
  (with-out-str (pprint/pprint edn)))

(defn- docker-run
  [command]
  (if-not HL_NO_DOCKER?
    (apply shell
           (concat
            ["docker run --rm"
             "-e" "AWS_CREDENTIAL_PROFILES_FILE=/.aws/credentials"
             "-e" "AWS_CONFIG_FILE=/.aws/config"
             "-e" "AWS_SHARED_CREDENTIALS_FILE=/.aws/credentials"
             "-v" (str (.getAbsolutePath (io/file "")) ":/project")
             "-v" (str AWS_DIR ":" "/.aws:ro")]
             (when-let [aws-access-key @AWS_ACCESS_KEY_ID]
               ["-e" (str "AWS_ACCESS_KEY_ID=" aws-access-key)])
             (when-let [aws-secret-access-key @AWS_SECRET_ACCESS_KEY]
               ["-e" (str "AWS_SECRET_ACCESS_KEY=" aws-secret-access-key)])
            (when DOCKER_NETWORK [(str "--network=" DOCKER_NETWORK)])
            (vec (flatten (mapv (fn [path] ["-v" path]) DOCKER_VOLUMES)))
            ["--user" USER_GID
             (str "-i" (when TTY? "t")) IMAGE_CORDS
             "/bin/bash" "-c" command]))
    (shell "bash" "-c" command)))

(defn docker:run
  "     \033[0;31m>\033[0m Run command in \033[0;31mfierycod/graalvm-native-image\033[0m docker context \n\n----------------------------------------------------------------\n"
  [command]
  (if HL_NO_DOCKER?
    (do
      (hpr (pre "Command") (accent "docker:run") (pre "is not available when environment variable") (accent "HL_NO_DOCKER") (pre "is set to true!"))
      (System/exit 1))
    (docker-run command)))

(defn deps-sync--babashka
  []
  (when-not (empty? (:pods (:runtime OPTIONS)))
    (hpr "Babashka pods found! Syncing" (str (accent "babashka pods") ".") "Pods should be distributed via a layer which points to" (accent ".holy-lambda/pods"))
    (docker-run "download_pods")
    (when (file-exists? ".holy-lambda/.babashka")
      (shell "rm -Rf .holy-lambda/pods")
      (shell "mkdir -p .holy-lambda/pods")
      (shell "cp -R .holy-lambda/.babashka .holy-lambda/pods/"))))

(def tasks-deps-edn
  {:mvn/local-repo ".holy-lambda/.m2"
   :aliases {:uberjar {:replace-deps {'com.github.seancorfield/depstar {:mvn/version "2.0.216"}}
                       :exec-fn 'hf.depstar/uberjar
                       :exec-args {}}}})

(defn deps-sync--deps
  []
  (stat-file "deps.edn")
  (hpr "Syncing project and holy-lambda" (accent "deps.edn"))
  (docker-run (str "clojure -A:" (and CLJ_ALIAS (str CLJ_ALIAS ":")) "uberjar -P"))
  (deps-sync--babashka))

(defn stack:sync
  "     \033[0;31m>\033[0m Syncs project & dependencies from either:
       \t\t        - \033[0;31m<Clojure>\033[0m  deps.edn
       \t\t        - \033[0;31m<Babashka>\033[0m bb.edn:runtime:pods"
  []
  (print-task "stack:sync")
  (when (and (not HL_NO_DOCKER?)
             (not (file-exists? ".holy-lambda/clojure/deps.edn")))
    (hpr "Project not synced yet. Syncing with docker image!")
    (let [cid (gensym "holy-lambda")]
      (shs "docker" "create" "--user" USER_GID "-ti" "--name" (str cid)  IMAGE_CORDS "bash")
      (shs "docker" "cp" (str cid ":/project/.holy-lambda") ".")
      (shs "docker" "rm" "-f" (str cid))))

  (if HL_NO_DOCKER?
    (shell "mkdir -p .holy-lambda")
    (do
      (when-not (file-exists? ".holy-lambda")
        (hpr (pre "Unable to sync docker image content with") (accent ".holy-lambda") (pre "project directory!")))

      (when-not (file-exists? ".holy-lambda/clojure")
        (hpr (pre "Project did not sync properly. Remove .holy-lambda directory and run") (accent "stack:sync")))

      ;; Correct holy-lambda deps.edn
      (spit HOLY_LAMBDA_DEPS_PATH (edn->pp-sedn tasks-deps-edn))))

  ;; Sync
  (deps-sync--deps)

  (hpr "Sync completed!"))

(defn stack-files-check--native
  []
  (when-not (file-exists? ".holy-lambda/build/latest.zip")
    (hpr (pre "No") (accent ".holy-lambda/build/latest.zip") (pre "found! Run") (accent "native:executable"))
    (System/exit 1)))

(defn stack-files-check--jar
  []
  (when-not (file-exists? OUTPUT_JAR_PATH)
    (hpr (pre "No") (accent OUTPUT_JAR_PATH) (pre "found! Run") (accent "stack:compile"))
    (System/exit 1)))

(defn stack-files-check
  [& [check]]
  (when-not (file-exists? ".holy-lambda")
    (hpr (pre "No") (accent ".holy-lambda") (pre "directory! Run") (accent "stack:sync"))
    (System/exit 1))

  (case check
    :native (do
              (stack-files-check--jar)
              (stack-files-check--native))
    :clojure (stack-files-check--jar)
    :babashka nil
    nil))

(defn build-stale?
  []
  (boolean (seq (fs/modified-since OUTPUT_JAR_PATH (fs/glob "src" "**/**.{clj,cljc,cljs}")))))

(defn native:conf
  "     \033[0;31m>\033[0m Provides native configurations for the application"
  []
  (print-task "native:conf")
  (exit-if-not-synced!)
  (stack-files-check :default)

  (io/make-parents (str NATIVE_CONFIGURATIONS_PATH "/traces.json"))

  (hpr "Compiling with agent support!")
  (shell "rm -Rf .cpcache .holy-lambda/build/output-agent.jar")
  (docker-run (str "USE_AGENT_CONTEXT=true clojure -X:uberjar :aliases '" (str [CLJ_ALIAS_KEY]) "' :aot '[\"" (str ENTRYPOINT) "\"]' " ":jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar " OUTPUT_JAR_PATH_WITH_AGENT " :main-class " (str ENTRYPOINT)))

  (hpr "Generating traces to ignore unnecessary reflection entries!")
  (docker-run (str JAVA_COMMAND
                   " -agentlib:native-image-agent="
                   "trace-output=" (str NATIVE_CONFIGURATIONS_PATH "/traces.json")
                   " "
                   "-Dexecutor=native-agent -jar " OUTPUT_JAR_PATH_WITH_AGENT))

  (hpr "Generating native-configurations!")
  (docker-run (str JAVA_COMMAND
                   " -agentlib:native-image-agent="
                   "config-merge-dir=" NATIVE_CONFIGURATIONS_PATH
                   " "
                   "-Dexecutor=native-agent -jar " OUTPUT_JAR_PATH_WITH_AGENT))

  (hpr "Cleaning up reflection-config.json!")
  (refl/clean-reflection-config!)

  (if-not (file-exists? NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH)
    (hpr (pre "Native configurations generation failed!"))
    (let [resource-config (json/parse-string (slurp (io/file NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH)))]
      (hpr "Cleaning up resource-config.json!")
      (spit NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH
            (json/generate-string
             (update-in resource-config
                        ["resources" "includes"]
                        (fn [patterns]
                          (filterv
                           (fn [p]
                             (let [patternv (get p "pattern")]
                               (not (or (s/includes? patternv ".class")
                                        (s/includes? patternv ".clj")
                                        (s/includes? patternv "native-agents-payloads")))))
                           patterns)))
             {:pretty true})))))

(def -bootstrap-file
"#!/bin/sh

export DISABLE_SIGNAL_HANDLERS=\"true\"
set -e

./output")

(defn bootstrap-file
  []
  (or (when (file-exists? BOOTSTRAP_FILE)
       (slurp BOOTSTRAP_FILE))
      -bootstrap-file))

(defn native:executable
  "     \033[0;31m>\033[0m Provides native executable of the application
\n----------------------------------------------------------------\n"
  [& args]
  (print-task "native:executable")
  (exit-if-not-synced!)
  (stack-files-check--jar)

  (when (build-stale?)
    (hpr (prw "Build is stale. Consider recompilation via") (accent "stack:compile")))

  (when-not (file-exists? NATIVE_CONFIGURATIONS_PATH)
    (hpr (prw "No native configurations has been generated. Native image build may fail. Run") (accent "native:conf") (prw "to generate native configurations.")))

  ;; Copy then build
  (shell-no-exit "bash -c \"[ -d resources/native-configuration ] && cp -rf resources/native-configuration .holy-lambda/build/\"")

  (docker-run (str "cd .holy-lambda/build/ && " NATIVE_IMAGE_COMMAND " -jar output.jar -H:ConfigurationFileDirectories=native-configuration"
                   (when NATIVE_IMAGE_ARGS
                     (str " " NATIVE_IMAGE_ARGS))))

  (if-not (file-exists? ".holy-lambda/build/output")
    (hpr (pre "Native image failed to create executable. Fix your build! Skipping next steps"))
    (do
      (spit ".holy-lambda/build/bootstrap" (bootstrap-file))
      (when (and NATIVE_DEPS_PATH (file-exists? NATIVE_DEPS_PATH))
        (hpr "Copying" (accent ":runtime:native-deps"))
        (shell (str "cp -R " NATIVE_DEPS_PATH " .holy-lambda/build/")))
      (hpr "Bundling artifacts...")
      (shell "bash -c \"cd .holy-lambda/build && chmod +x bootstrap\"" )
      (shell "bash -c \"cd .holy-lambda/build && rm -Rf output-agent.jar native-configuration resources/native-configuration resources/native-agents-payloads output.build_artifacts.txt\"")
      (shell "bash -c \"cd .holy-lambda/build && zip -r latest.zip . -x 'output.jar'\""))))

(defn stack:compile
  "     \033[0;31m>\033[0m Compiles sources if necessary
  \t\t        - \033[0;31m:force\033[0m         - force compilation even if sources did not change"
  [& args]
  (print-task "stack:compile")
  (exit-if-not-synced!)
  (let [{:keys [force]} (norm-args args)])
  (when (and (not (build-stale?)) (not force))
    (hpr "Nothing to compile. Sources did not change!")
    (System/exit 0))
  (docker-run (str "clojure -X:uberjar :aliases '" (str [CLJ_ALIAS_KEY]) "' :aot '[\"" (str ENTRYPOINT) "\"]' " ":jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar " OUTPUT_JAR_PATH " :main-class " (str ENTRYPOINT))))

(defn mvn-local-test
  [file]
  (if (not= (:mvn/local-repo (edn/read-string (slurp (io/file file)))) ".holy-lambda/.m2")
    (hpr (pre "property") (accent ":mvn/local-repo") (pre "in file") (accent file) (pre "should be set to") (accent ".holy-lambda/.m2"))
    (hpr (prs "property") (accent ":mvn/local-repo") (prs "in file") (accent file) (prs "is correct"))))

(defn stack:doctor
  "     \033[0;31m>\033[0m Diagnoses common issues of holy-lambda stack"
  []
  (let [exit-code (atom 0)
        exit-code-err! #(reset! exit-code 1)]
    (print-task "stack:doctor")
    (do
      (println "")
      (hpr "---------------------------------------")
      (hpr " Checking health of holy-lambda stack")
      (hpr " Home directory is:       " (accent HOME_DIR))
      (hpr " Project directory is:    " (accent PROJECT_DIRECTORY))
      (hpr " AWS SAM version:         " (accent (or (s/trim (shs-no-err "sam" "--version")) "UNKNOWN")))
      (hpr " AWS CLI version:         " (accent (or (s/trim (shs-no-err "aws" "--version")) "UNKNOWN")))
      (hpr " AWS directory is:        " (accent AWS_DIR))
      (hpr " AWS directory exists?:   " (accent AWS_DIR_EXISTS?))
      (hpr " Docker version:          " (accent (or (s/trim (shs-no-err "docker" "--version")) "UNKNOWN")))
      (hpr " Babashka tasks sha:      " (accent (or (:sha (get (:deps BB_EDN) 'io.github.FieryCod/holy-lambda-babashka-tasks)) "LOCAL")))
      (hpr " Babashka tasks version:  " (accent TASKS_VERSION))
      (hpr " Babashka version:        " (accent (or (s/trim (shs-no-err "bb" "version")) "UNKNOWN")))
      (hpr " TTY:                     " (accent TTY?))
      (hpr " Entrypoint:              " (accent ENTRYPOINT))
      (hpr "---------------------------------------\n"))

    (when-not (file-exists? AWS_DIR)
      (hpr (pre "$HOME/.aws does not exists. Did you run") (accent "aws configure"))
      (exit-code-err!))

    (when-not (file-exists? "deps.edn")
      (hpr (pre "File deps.edn does not exists!"))
      (exit-code-err!))

    (if-not ENTRYPOINT
      (hpr (pre ":runtime:entrypoint is required!"))
      (hpr (prs ":runtime:entrypoint looks good")))

    (when (and NATIVE_DEPS_PATH
               (not (file-exists? NATIVE_DEPS_PATH)))
      (hpr (prw ":runtime:native-deps folder does not exists") (accent ":native:executable") (prw "will not include any extra deps!")))

    (mvn-local-test "deps.edn")
    (mvn-local-test "bb.edn")

    (if (file-exists? HOLY_LAMBDA_DEPS_PATH)
      (hpr (prs "Syncing stack is not required"))
      (do
        (hpr (pre "Stack is not synced! Run:") (accent "stack:sync"))
        (exit-code-err!)))

    (if-let [cmds-not-found (seq (filter (comp not command-exists?) REQUIRED_COMMANDS))]
      (do
        (hpr (str (pre (str "Commands " cmds-not-found " not found. Install all then run: ")) (underline "bb doctor")))
        (exit-code-err!))
      (do
        (hpr (prs "Required commands") (accent (str REQUIRED_COMMANDS)) (prs "installed!"))))
    (System/exit @exit-code)))

(defn stack:version
  "     \033[0;31m>\033[0m Outputs holy-lambda babashka tasks version"
  []
  (print-task "stack:version")
  (hpr (str (prs "Current tasks version is: ") (accent TASKS_VERSION)))
  (when-not (= (s/trim (:body (curl/get REMOTE_TASKS))) TASKS_VERSION)
    (hpr (pre "Local version of tasks does not match stable tasks version. Update tasks sha!"))
    (System/exit 1)))

(defn stack:purge
  "     \033[0;31m>\033[0m Purges build artifacts"
  []
  (print-task "stack:purge")
  (let [artifacts [".holy-lambda"
                   ".cpcache"
                   "node_modules"]]

    (hpr  (str "Purging build artifacts:" "\n\n" (plist artifacts)))

    (doseq [art artifacts]
      (shell (str "rm -rf " art)))

    (hpr  (prs "Build artifacts purged"))))
