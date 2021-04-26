(ns holy-lambda.tasks
  "This namespace contains tasks!"
  (:require
   [clojure.string :as s]
   [clojure.pprint :as pprint]
   [clojure.java.shell :as csh]
   [clojure.edn :as edn]
   [babashka.tasks :as tasks]
   [babashka.deps :as deps]
   [babashka.fs :as fs]
   [babashka.curl :as curl]
   [babashka.process :as p]
   [clojure.java.io :as io]))

(def TASK_NAME (or (resolve 'babashka.tasks/*-task-name*)
                   (resolve 'babashka.tasks/*task-name*)))

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

;;;; [START] HELPERS
(defn- exit-non-zero
  [proc]
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell [cmd & args]
  (exit-non-zero (p/process (into (p/tokenize cmd) args) {:inherit true})))

(defn- clojure [cmd & args]
  (exit-non-zero (deps/clojure (into (p/tokenize cmd) args))))

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

(defn command-exists?
  [cmd]
  (= (int (:exit (csh/sh "which" cmd))) 0))

(defn plist
  [xs]
  (s/join "" (mapv (fn [x]
                     (str " - " x "\n"))
                   xs)))
;;;; [END] HELPERS

(def AVAILABLE_RUNTIMES #{:babashka :native :java})
(def AVAILABLE_BUILD_TOOLS #{:lein :deps})
(def AVAILABLE_REGIONS #{"us-east-2", "us-east-1", "us-west-1", "us-west-2", "af-south-1", "ap-east-1", "ap-south-1", "ap-northeast-3", "ap-northeast-2", "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ca-central-1", "cn-north-1", "cn-northwest-1", "eu-central-1", "eu-west-1", "eu-west-2", "eu-south-1", "eu-west-3", "eu-north-1", "me-south-1", "sa-east-1"})
(def REMOTE_TASKS "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/modules/holy-lambda-babashka-tasks/src/holy_lambda/tasks.clj")
(def TASKS_VERSION "0.0.1")
(def TASKS_VERSION_MATCH #"(?:TASKS_VERSION) (\"[0-9]*\.[0-9]*\.[0-9]*\")")
(def BUCKET_IN_LS_REGEX #"(?:[0-9- :]+)(.*)")

(defn options
  []
  (:holy-lambda/options (edn/read-string (slurp (io/file "bb.edn")))))

(def OPTIONS
  (try
    (options)
    (catch Exception err_
      (hpr (pre "Either bb.edn not found or does not contain :holy-lambda/options"))
      (System/exit 1))))

(defn stat-file
  [filename]
  (when-not (fs/exists? (io/file filename))
    (hpr "PATH" (accent filename) "does not exists.. Exiting!")
    (System/exit 1)))

(alter-var-root #'babashka.tasks/-log-info
                (fn [f]
                  (fn [& strs]
                    (hpr (str "Command " (red "<") (accent @TASK_NAME) (red ">"))))))

(def BUILD_TOOL (:build-tool OPTIONS))
(def ENVS_FILE
  (try
    (if-not (fs/exists? (io/file (:envs-file OPTIONS)))
      (throw (Exception. "."))
      (:envs-file OPTIONS))
    (catch Exception err_
      (hpr (pre "File envs.json for aws sam not found.. Exiting!\n
Check https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-using-invoke.html#serverless-sam-cli-using-invoke-environment-file"))
      (System/exit 1))))

(def IMAGE_NAME
  (case (:image-tag OPTIONS)
    :ce "fierycod/graalvm-native-image:ce"
    :ee "fierycod/graalvm-native-image:ee"
    (do (hpr (str (pre "Incorrect image tag choosen: ") (:image-tag OPTIONS)))
        (System/exit 1))))

(def INFRA (:infra OPTIONS))
(def RUNTIME (:runtime OPTIONS))
(def RUNTIME_NAME (:name RUNTIME))
(def BUCKET_PREFIX (:bucket-prefix INFRA))
(def BUCKET_NAME (:bucket-name INFRA))
(def REGION (:region INFRA))

(def USER_GID
  (str (s/trim (shs "id -u"))
       ":" (s/trim (shs "id -g"))))

(def HOME_DIR
  (.getAbsolutePath
   (io/file (or
             (System/getenv "XDG_CACHE_HOME")
             (System/getProperty "user.home")))))

(def AWS_DIR
  (.getAbsolutePath (io/file HOME_DIR ".aws")))

(defn docker:run
  "     \033[0;31m>\033[0m Run command in \033[0;31mfierycod/graalvm-native-image\033[0m docker context"
  [command]
  (shell "docker run --rm"
         "-e" "AWS_CREDENTIAL_PROFILES_FILE=/project/.aws/credentials"
         "-e" "AWS_CONFIG_FILE=/project/.aws/config"
         "-v" (str (.getAbsolutePath (io/file "")) ":/project")
         "-v" (str AWS_DIR ":" "/project/.aws:ro")
         "--user" USER_GID
         "-it" IMAGE_NAME
         "/bin/bash" "-c" command))

(defn buckets
  []
  (set (mapv (fn [b] (some-> (re-find BUCKET_IN_LS_REGEX b) second))
             (s/split (shs "aws" "s3" "ls") #"\n"))))

(defn bucket-exists?
  []
  (contains? (buckets) BUCKET_NAME))

(defn modify-lambda-options
  [modify-fn]
  (spit "bb.edn" (modify-fn OPTIONS)))

(defn deps-sync--babashka
  []
  (when (= RUNTIME_NAME :babashka)
    (when-not (empty? (:pods (:runtime OPTIONS)))
      ;; (let [pods ]
        (docker:run "download_pods" ))
      ;; )
    )
  )

(defn deps-sync--deps
  []
  (stat-file "deps.edn")
  (shsp "clojure" "-P")
  (deps-sync--babashka))

(defn deps-sync--lein
  []
  (stat-file "project.clj")
  (shsp "lein" "deps")
  (deps-sync--babashka))

(defn stack:sync
  "     \033[0;31m>\033[0m Syncs dependencies from either:
       \t\t        - \033[0;31m<Clojure>\033[0m  project.clj
       \t\t        - \033[0;31m<Clojure>\033[0m  deps.edn
       \t\t        - \033[0;31m<Babashka>\033[0m bb.edn:runtime:pods"
  []
  (case BUILD_TOOL
    :lein  (deps-sync--lein)
    :deps  (deps-sync--deps)
    (do
      (hpr (str (pre "Incorrect build tool used: ") (accent BUILD_TOOL)))
      (System/exit 1))))

(defn stack:invoke
  "     \033[0;31m>\033[0m Invokes lambda fn (check sam local invoke --help):
       \t\t        - \033[0;31m:name\033[0m   - either \033[0;31m:name\033[0m or \033[0;31m:default-lambda-fn-name\033[0m
       \t\t        - \033[0;31m:e\033[0m      - fetch logs up to this time
       \t\t        - \033[0;31m:s\033[0m      - fetch logs starting at this time
       \t\t        - \033[0;31m:filter\033[0m - find logs that match terms "
  []
  (hpr "Checking health of holy-lambda stack"))

(defn tasks:doctor
  "     \033[0;31m>\033[0m Diagnoses common issues of holy-lambda stack"
  []
  (hpr "Checking health of holy-lambda stack")
  (hpr "Home directory is:" (accent HOME_DIR))
  (hpr "AWS directory is:" (accent AWS_DIR))

  (stat-file AWS_DIR)

  (if-not (contains? AVAILABLE_RUNTIMES RUNTIME_NAME)
    (do
      (hpr (str (pre ":runtime ") (accent RUNTIME_NAME) (pre " is not supported!")))
      (hpr (str "Choose one of supported build tools: " AVAILABLE_RUNTIMES)))
    (hpr (prs ":runtime looks good")))

  (if-not (contains? AVAILABLE_REGIONS REGION)
    (do
      (hpr (str (pre "Region ") (accent REGION) (pre " is not supported!")))
      (hpr (str "Choose one of supported regions:\n" (with-out-str (pprint/pprint AVAILABLE_REGIONS)))))
    (hpr (prs ":infra:region definition looks good")))

  (if (s/includes? BUCKET_PREFIX "_")
    (hpr (pre ":infra:bucket-prefix should not contain any of _ characters"))
    (hpr (prs ":infra:bucket-prefix looks good")))

  (if (s/includes? BUCKET_NAME "_")
    (hpr (pre ":infra:bucket-name should not contain any of _ characters"))
    (hpr (str (prs ":infra:bucket-name looks good")
         (when-not (bucket-exists?)
           (str (prs ", but ") (accent BUCKET_NAME) (prw " does not exists (use bb :bucket:create)"))))))

  (if-not (contains? AVAILABLE_BUILD_TOOLS BUILD_TOOL)
    (do
      (hpr (str (pre ":build-tool ") (accent BUILD_TOOL) (pre " is not supported!")))
      (hpr (str "Choose one of supported build tools: " AVAILABLE_BUILD_TOOLS)))
    (hpr (prs ":build-tool looks good")))

  (if-let [cmds-not-found (seq (filter (comp not command-exists?) ["aws" "sam" "bb" "docker" "clojure" "zip" "id"]))]
    (hpr (str (pre (str "Commands " cmds-not-found " not found. Install all then run: ")) (underline "bb doctor")))
    (do
      (hpr (prs "All necessary holy-lambda dependencies installed"))
      (shell "sam validate"))))

(defn norm-args
  [args]
  (into {} (mapv
            (fn [[k v]]
              [(cond-> k
                 (s/includes? k ":")
                 (subs 1)

                 true keyword)
                 (or v true)])
            (partition-all 2 args))))

(defn stack:logs
  "     \033[0;31m>\033[0m Possible arguments (check sam logs --help):
       \t\t        - \033[0;31m:name\033[0m   - either \033[0;31m:name\033[0m or \033[0;31m:default-lambda-fn-name\033[0m
       \t\t        - \033[0;31m:e\033[0m      - fetch logs up to this time
       \t\t        - \033[0;31m:s\033[0m      - fetch logs starting at this time
       \t\t        - \033[0;31m:filter\033[0m - find logs that match terms "
  [& args]
  (let [{:keys [name tail s e filter]} (norm-args args)]
    (shsp "sam" "logs"
         "-n" (or name (:default-lambda-fn-name OPTIONS))
         (when s "-s") (when s s)
         (when e "-e") (when e e)
         (when filter "--filter") (when filter filter)
         (when tail "-t"))))

(defn local-tasks-match-remote?
  []
  (= (s/replace
      (second (re-find
               TASKS_VERSION_MATCH
               (:body (curl/get REMOTE_TASKS))))
      "\""
      "")
     TASKS_VERSION))

(defn tasks:version
  "     \033[0;31m>\033[0m Outputs holy-lambda babashka tasks version"
  []
  (hpr (str (prs "Current tasks version is: ") (accent TASKS_VERSION)))
  (when-not (local-tasks-match-remote?)
    (hpr "There is newer version of tasks on remote. Please update tasks :sha")))

(defn stack:purge
  "     \033[0;31m>\033[0m Purges build artifacts"
  []
  (let [artifacts ["packaged.yml"
                   "target"
                   ".aws"
                   "packaged-native-yml"
                   "output"
                   "latest.zip"
                   "BABASHKA_ENTRYPOINT"
                   "bootstrap"
                   "Dockerfile.ee"
                   "node_modules"]]

    (hpr  (str (accent "Purging build artifacts:") "\n\n" (plist artifacts)))

    (doseq [art artifacts]
      (shell (str "rm -rf " art)))

    (hpr  (prs "Build artifacts purged"))))

(defn docker:build:ee
  "     \033[0;31m>\033[0m Builds local image for GraalVM EE "
  []
  (hpr  (accent "Building GraalVM EE docker image"))
  (spit "Dockerfile.ee" (:body (curl/get "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/docker/ee/Dockerfile")))
  (shell "docker build . -f Dockerfile.ee -t fierycod/graalvm-native-image:ee")
  (shell "rm -rf Dockerfile.ee"))

(defn -create-bucket
  []
  (shsp "aws" "s3" "mb" (str "s3://" BUCKET_NAME)))

(defn -remove-bucket
  []
  (shsp "aws" "s3" "rb" (str "s3://" BUCKET_NAME)))

(defn bucket:create
  "     \033[0;31m>\033[0m Creates a s3 bucket using \033[0;31m:bucket-name\033[0m"
  []
  (if (bucket-exists?)
    (hpr (prs "Bucket") (accent BUCKET_NAME) " already exists!")
    (do (hpr (prs "Creating a bucket") (accent BUCKET_NAME))
        (-create-bucket))))

(defn bucket:remove
  "     \033[0;31m>\033[0m Removes a s3 bucket using \033[0;31m:bucket-name\033[0m"
  []
  (if-not (bucket-exists?)
    (hpr (pre "Bucket") (accent BUCKET_NAME) " does not exists! Not removing")
    (do (hpr (prs "Removing a bucket") (accent BUCKET_NAME))
        (-remove-bucket))))


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

;; (defn docker []
;;   (shell "docker build -t uochan/antq ."))

;; (defn docker-test []
;;   (shell "docker run --rm -v"
;;          (str (fs/absolutize "") ":/src")
;;          "-w" "/src" "uochan/antq:latest"))

(defn coverage
  "Run test coverage."
  []
  (clojure "-M:coverage:dev:nop --src-ns-path=src --test-ns-path=test --codecov"))
