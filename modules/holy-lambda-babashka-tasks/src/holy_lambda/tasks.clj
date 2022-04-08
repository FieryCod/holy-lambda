(ns holy-lambda.tasks
  (:require
   [clojure.string :as s]
   [clojure.pprint :as pprint]
   [clojure.java.shell :as csh]
   [clojure.edn :as edn]
   [cheshire.core :as json]
   [babashka.deps :as deps]
   [babashka.curl :as curl]
   [babashka.fs :as fs]
   [babashka.process :as p]
   [clojure.java.io :as io]
   [holy-lambda.refl :as refl])
  (:refer-clojure :exclude [spit]))

(deps/add-deps '{:deps {borkdude/rewrite-edn {:mvn/version "0.0.2"}}})
(require '[borkdude.rewrite-edn :as r])

;; Coloring taken from https://github.com/trhura/clojure-term-colors/blob/master/src/clojure/term/colors.clj
;; Included as is to prevent the syncing of deps
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

(defn env-true?
  [env]
  (when-let [prop (System/getenv env)]
    (contains? #{"true" "1"} prop)))

(def HL_DEBUG? (env-true? "HL_DEBUG"))

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
  (let [part-args (partition-all 2 args)
        args (mapcat (fn [[k v]]
                       (if (and (and k (or (s/includes? k "--")
                                           (s/includes? k ":")))
                                (and v (or (s/includes? v "--")
                                           (s/includes? v ":"))))
                         [k true v true]
                         [k v]))
                     part-args)]
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
           (partition-all 2 args)))))

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
(def REMOTE_TASKS_SHA "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/modules/holy-lambda-babashka-tasks/STABLE_VERSION_SHA")

(defn new-available-tasks-version
  []
  (s/trim (:body (curl/get REMOTE_TASKS_SHA))))

(def BB_EDN_STRING (slurp (io/file "bb.edn")))

(defn bb-edn
  []
  (edn/read-string BB_EDN_STRING))

(def BB_EDN
  (try
    (bb-edn)
    (catch Exception err
      (do
        (hpr (pre "File") (accent "bb.edn") (pre "not found?!")
             (str "\n" (pre "Original message: ") (pre (.getMessage err))))
        (System/exit 1)))))

(def TASKS_VERSION_SHA (or (:sha (get (:deps BB_EDN) 'io.github.FieryCod/holy-lambda-babashka-tasks)) "LOCAL"))
(def OPTIONS
  (if-let [opts (:holy-lambda/options BB_EDN)]
    opts
    (do (hpr (accent ":holy-lambda/options") (pre "are not declared in") (accent "bb.edn") (pre "file!")
             (pre "Exiting!"))
        (System/exit 1))))

(def TTY? (= (:exit @(shell-no-exit "test" "-t" "1")) 0))
(def DOCKER (:docker OPTIONS))
(def BUILD (:build OPTIONS))
(def ^:dynamic HL_NO_DOCKER?
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
(when CLJ_ALIAS_KEY
  (hpr (pre (str "HL_CLJ_ALIAS and :clj-alias are deprecated. Use HL_COMPILE_CMD or :compile-cmd instead"))))

(def COMPILE_CMD (or (System/getenv "HL_COMPILE_CMD")
                     (:compile-cmd BUILD)))

(def COMPILE_CMD_ERROR (pre "Neither HL_COMPILE_CMD nor :build:compile-cmd has been specified!"))

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
  (hpr "Pulling the image" (accent IMAGE_CORDS) "from" (str (accent "Container Registry") "!"))
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
(def RUNTIME (or (:runtime OPTIONS)
                 (:backend OPTIONS)))
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

(def OUTPUT_JAR_PATH ".holy-lambda/build/output.jar")
(def OUTPUT_JAR_PATH_WITH_AGENT ".holy-lambda/build/output-agent.jar")
(def REQUIRED_COMMANDS ["aws" "sam" "bb" "docker" "clojure" "zip" "id" "bash"])
(def NATIVE_CONFIGURATIONS_PATH "resources/native-configuration")
(def NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH "resources/native-configuration/resource-config.json")
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

(defn hl:docker:run
  "     \033[0;31m>\033[0m Run command docker context \n\n----------------------------------------------------------------\n"
  [command]
  (if HL_NO_DOCKER?
    (do
      (hpr (pre "Command") (accent "hl:docker:run") (pre "is not available when environment variable") (accent "HL_NO_DOCKER") (pre "is set to true!"))
      (System/exit 1))
    (docker-run command)))

(defn hl:update-bb-tasks
  "     \033[0;31m>\033[0m Update \033[0;31m:sha\033[0m of \033[0;31mio.github.FieryCod/holy-lambda-babashka-tasks\033[0m to latest stable version"
  []
  (print-task "hl:update-bb-tasks")
  (let [new-version (new-available-tasks-version)]
    (if (= new-version TASKS_VERSION_SHA)
      (hpr "You're using the latest tasks :sha. No update necessary!")
      (do
        (hpr "Updating from tasks version:" (accent TASKS_VERSION_SHA) "to:" (accent (new-available-tasks-version)))
        (let [edn (r/parse-string BB_EDN_STRING)]
          (spit "bb.edn" (r/update-in edn [:deps 'io.github.FieryCod/holy-lambda-babashka-tasks]
                                      (fn [x]
                                        (if-not (:sha (r/sexpr x))
                                          x
                                          (assoc (r/sexpr x) :sha new-version))))))))))

(defn deps-sync--babashka
  []
  (shell "mkdir -p .holy-lambda/bb-clj-deps")
  (shell "bb" "--uberjar" ".holy-lambda/bb-clj-deps/.m2/libs.jar")
  (shell "bash -c \"cd .holy-lambda/bb-clj-deps/.m2 && unzip -oq libs.jar && rm -Rf libs.jar\"")
  (when-not (empty? (:pods RUNTIME))
    (hpr "Babashka pods found! Syncing" (str (accent "babashka pods") ".") "Pods should be distributed via a layer which points to" (accent ".holy-lambda/pods"))
    (docker-run "cd / && download_pods")
    (when (file-exists? ".holy-lambda/.babashka")
      (shell "rm -Rf .holy-lambda/pods")
      (shell "mkdir -p .holy-lambda/pods")
      (shell "cp -R .holy-lambda/.babashka .holy-lambda/pods/"))))

(defn hl:babashka:sync
  "     \033[0;31m>\033[0m Syncs dependencies from \033[0;31mbb.edn\033[0m \033[0;31m:backend:pods\033[0m and \033[0;31m:deps\033[0m to \033[0;31m.holy-lambda\033[0m directory. "
  []
  (print-task "hl:babashka:sync")
  (deps-sync--babashka)
  (hpr "Babashka deps & pods sync completed"))

(defn stack-files-check--jar
  []
  (when-not (file-exists? OUTPUT_JAR_PATH)
    (hpr (pre "No") (accent OUTPUT_JAR_PATH) (pre "found! Run") (accent "hl:compile"))
    (System/exit 1)))

(defn build-stale?
  []
  (if-not (file-exists? "src")
    true
    (boolean (seq (fs/modified-since OUTPUT_JAR_PATH (fs/glob "src" "**/**.{clj,cljc,cljs}"))))))

(defn hl:native:conf
  "     \033[0;31m>\033[0m Provides native configurations for the application"
  []
  (print-task "hl:native:conf")
  (stack-files-check--jar)
  (io/make-parents (str NATIVE_CONFIGURATIONS_PATH "/traces.json"))
  (when (build-stale?)
    (hpr (prw "Build is stale. Consider recompilation via") (accent "hl:compile")))
  (when-not COMPILE_CMD (hpr COMPILE_CMD_ERROR))
  (shell "bash -c \"rm -Rf .cpcache .holy-lambda/build/output-agent.jar && mv .holy-lambda/build/output.jar .holy-lambda/build/output-temp.jar\"")
  (hpr "Compiling with agent support!")
  (binding [HL_NO_DOCKER? true] (docker-run (str "USE_AGENT_CONTEXT=true " COMPILE_CMD)))
  (shell "bash -c \"mv .holy-lambda/build/output.jar .holy-lambda/build/output-agent.jar && mv .holy-lambda/build/output-temp.jar .holy-lambda/build/output.jar\"")
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
  -bootstrap-file)

(defn hl:native:executable
  "     \033[0;31m>\033[0m Provides native executable of the application
\n----------------------------------------------------------------\n"
  [& args]
  (print-task "hl:native:executable")
  (stack-files-check--jar)

  (when (build-stale?)
    (hpr (prw "Build is stale. Consider recompilation via") (accent "hl:compile")))

  (when-not (file-exists? NATIVE_CONFIGURATIONS_PATH)
    (hpr (prw "No native configurations has been generated. Native image build may fail. Run") (accent "native:conf") (prw "to generate native configurations.")))
  (shell-no-exit "bash -c \"[ -d resources/native-configuration ] && cp -rf resources/native-configuration .holy-lambda/build/\"")
  (docker-run (str "cd .holy-lambda/build/ && " NATIVE_IMAGE_COMMAND " -jar output.jar -H:ConfigurationFileDirectories=native-configuration"
                   (when NATIVE_IMAGE_ARGS
                     (str " " NATIVE_IMAGE_ARGS))))
  (if-not (file-exists? ".holy-lambda/build/output")
    (hpr (pre "Native image failed to create executable. Fix your build! Skipping next steps"))
    (do
      (spit ".holy-lambda/build/bootstrap" (bootstrap-file))
      (when (and NATIVE_DEPS_PATH (file-exists? NATIVE_DEPS_PATH))
        (hpr "Copying" (accent ":backend:native-deps"))
        (shell (str "cp -R " NATIVE_DEPS_PATH " .holy-lambda/build/")))
      (hpr "Bundling artifacts...")
      (shell "bash -c \"cd .holy-lambda/build && chmod +x bootstrap\"")
      (shell "bash -c \"cd .holy-lambda/build && rm -Rf output-agent.jar native-configuration resources/native-configuration resources/native-agents-payloads output.build_artifacts.txt\"")
      (shell "bash -c \"cd .holy-lambda/build && zip -r latest.zip . -x 'output.jar'\"")
      (hpr "Native artifact of the project is available at" (accent ".holy-lambda/build/latest.zip"))
      (hpr "Binary:" (accent ".holy-lambda/build/output"))
      (hpr "Bootstrap:" (accent ".holy-lambda/build/bootstrap")))))

(defn hl:compile
  "     \033[0;31m>\033[0m Compiles sources if necessary
  \t\t            - \033[0;31m:force\033[0m - force compilation even if sources did not change"
  [& args]
  (print-task "hl:compile")
  (let [{:keys [force]} (norm-args args)]
    (when (and (not (build-stale?)) (not force))
      (hpr "Nothing to compile. Sources did not change!")
      (System/exit 0))
    (binding [HL_NO_DOCKER? true]
      (when-not COMPILE_CMD
        (hpr (pre "Neither HL_COMPILE_CMD nor :build:compile-cmd has been specified!"))
        (System/exit 1))
      (docker-run COMPILE_CMD)
      (stat-file ".holy-lambda/build/output.jar")
      (hpr "Uberjar artifact of the project is available at" (accent ".holy-lambda/build/output.jar")))))


(defn hl:doctor
  "     \033[0;31m>\033[0m Diagnoses common issues in the project"
  []
  (let [exit-code (atom 0)
        exit-code-err! #(reset! exit-code 1)]
    (print-task "hl:doctor")
    (do
      (println "")
      (hpr "---------------------------------------")
      (hpr " Checking health of tools")
      (hpr "---------------------------------------")
      (hpr " Home directory is:       " (accent HOME_DIR))
      (hpr " Project directory is:    " (accent PROJECT_DIRECTORY))
      (hpr " AWS SAM version:         " (accent (or (s/trim (shs-no-err "sam" "--version")) "UNKNOWN")))
      (hpr " AWS CLI version:         " (accent (or (s/trim (shs-no-err "aws" "--version")) "UNKNOWN")))
      (hpr " AWS directory is:        " (accent AWS_DIR))
      (hpr " AWS directory exists?:   " (accent AWS_DIR_EXISTS?))
      (hpr " Docker version:          " (accent (or (s/trim (shs-no-err "docker" "--version")) "UNKNOWN")))
      (hpr " Babashka tasks sha:      " (accent TASKS_VERSION_SHA))
      (hpr " Babashka version:        " (accent (or (s/trim (shs-no-err "bb" "version")) "UNKNOWN")))
      (hpr " Clojure version:         " (accent (or (s/trim (shs-no-err "clojure" "--version")) "UNKNOWN")))
      (hpr " Docker image:            " (accent (:image DOCKER)))
      (hpr " Java version:            " (accent (or (some-> (shs "java" "-version") (s/split-lines)
                                                            first
                                                            s/trim)
                                                    "UNKNOWN")))
      (hpr " TTY:                     " (accent TTY?))
      (hpr "---------------------------------------"))

    (when-not (file-exists? AWS_DIR)
      (hpr (pre "$HOME/.aws does not exists. Did you run") (accent "aws configure"))
      (exit-code-err!))

    (if-not (file-exists? "deps.edn")
      (do
        (hpr (pre "File deps.edn does not exists!"))
        (exit-code-err!))
      (do (hpr " --------------" (accent "deps.edn") "-------------")
        (pprint/pprint (edn/read-string (slurp "deps.edn")))
        (hpr " ---------------" (accent "deps.edn") "--------------")))

    (if-not (file-exists? "bb.edn")
      (do
        (hpr (pre "File bb.edn does not exists!"))
        (exit-code-err!))
      (do (hpr " --------------- " (accent "bb.edn") " --------------")
        (pprint/pprint (edn/read-string (slurp "bb.edn")))
        (hpr " ---------------- " (accent "bb.edn") " ---------------")))

    (when (and NATIVE_DEPS_PATH
               (not (file-exists? NATIVE_DEPS_PATH)))
      (hpr (prw ":backend:native-deps folder does not exists") (accent ":native:executable") (prw "will not include any extra deps!")))

    (if-let [cmds-not-found (seq (filter (comp not command-exists?) REQUIRED_COMMANDS))]
      (do
        (hpr (str (pre (str "Commands " cmds-not-found " not found. Install all then run: ")) (underline "bb hl:doctor")))
        (exit-code-err!))
      (do
        (hpr (prs "Required commands") (accent (str REQUIRED_COMMANDS)) (prs "installed!"))))
    (System/exit @exit-code)))

(defn hl:version
  "     \033[0;31m>\033[0m Outputs holy-lambda babashka tasks version"
  []
  (print-task "hl:version")
  (hpr (str (prs "Current tasks version is: ") (accent TASKS_VERSION_SHA)))
  (when-not (= (new-available-tasks-version) TASKS_VERSION_SHA)
    (hpr (prw "Local version of tasks does not match stable tasks version. Consider updating the tasks via `bb hl:update-bb-tasks`"))))

(defn hl:sync
  "     \033[0;31m>\033[0m DEPRECATED! See HL changelog"
  []
  (print-task "hl:sync")
  (hpr (pre "hl:sync is no-op. Clojure dependencies are now downloaded to $HOME/.m2 directory. Remove :mvn/local-repo from the deps.edn. For downloading babashka pods and dependencies execute hl:babashka:sync!")))

(defn hl:clean
  "     \033[0;31m>\033[0m Cleanes build artifacts"
  []
  (print-task "hl:clean")
  (let [artifacts [".holy-lambda"
                   ".cpcache"
                   "node_modules"]]

    (hpr  (str "Cleaning build artifacts:" "\n\n" (plist artifacts)))

    (doseq [art artifacts]
      (shell (str "rm -rf " art)))

    (hpr  (prs "Build artifacts cleaned"))))
