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
   [clojure.java.io :as io]))

(deps/add-deps {:deps {'clojure-term-colors/clojure-term-colors {:mvn/version "0.1.0"}}})

(require
 '[clojure.term.colors :refer [underline blue yellow red green]])

;;;; [START] HELPERS
(defn norm-args
  [args]
  (into {} (mapv
            (fn [[k v]]
              [(cond-> k
                 (s/includes? k ":")
                 (subs 1)

                 true
                 keyword)
               (or v true)])
            (partition-all 2 args))))

(defn- exit-non-zero
  [proc]
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell
  [cmd & args]
  (exit-non-zero (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit true})))

(defn- shell-no-exit
  [cmd & args]
  (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit true}))

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

(when-not (or (and (= OS :unix)
                   (= (:exit (csh/sh "pgrep" "-f" "docker")) 0))
              (and (= OS :mac)
                   (= (:exit (csh/sh "pgrep" "-f" "Docker.app")) 0)))
  (hpr (pre "Docker is not running! Enable and run docker first before using holy-lambda!"))
  (System/exit 1))

(def AVAILABLE_RUNTIMES #{:babashka :native :java})
(def AVAILABLE_REGIONS #{"us-east-2", "us-east-1", "us-west-1", "us-west-2", "af-south-1", "ap-east-1", "ap-south-1", "ap-northeast-3", "ap-northeast-2", "ap-southeast-1", "ap-southeast-2", "ap-northeast-1", "ca-central-1", "cn-north-1", "cn-northwest-1", "eu-central-1", "eu-west-1", "eu-west-2", "eu-south-1", "eu-west-3", "eu-north-1", "me-south-1", "sa-east-1"})
(def REMOTE_TASKS "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/modules/holy-lambda-babashka-tasks/src/holy_lambda/tasks.clj")
(def TASKS_VERSION "0.1.49")
(def TASKS_VERSION_MATCH #"(?:TASKS_VERSION) (\"[0-9]*\.[0-9]*\.[0-9]*\")")
(def BUCKET_IN_LS_REGEX #"(?:[0-9- :]+)(.*)")
(def LAYER_CACHE_DIRECTORY ".holy-lambda/.cache/layers")

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

(def DOCKER (:docker OPTIONS))
(def BUILD (:build OPTIONS))

(def CLJ_ALIAS_KEY (:clj-alias BUILD))
(when (and CLJ_ALIAS_KEY (not (keyword? CLJ_ALIAS_KEY)))
  (hpr (pre "Defined") (accent "build:clj-alias") (pre "should be a keyword")))

(when (and CLJ_ALIAS_KEY (not (CLJ_ALIAS_KEY (:aliases (read-string (slurp "deps.edn"))))))
  (hpr (pre "Defined")
       (accent "build:clj-alias")
       (accent CLJ_ALIAS_KEY)
       (pre "does not exists in") (accent "deps.edn")))

(def CLJ_ALIAS (some-> CLJ_ALIAS_KEY name
                       (str ":")))

(defn stat-file
  [filename]
  (when-not (fs/exists? (io/file filename))
    (hpr "PATH" (accent filename) "does not exists.. Exiting!")
    (System/exit 1)))

(defn print-task
  [task-name]
  (hpr (str "Command " (red "<") (accent task-name) (red ">"))))

(def STACK (:stack OPTIONS))
(def DEFAULT_ENVS_FILE
  (try
    (if-not (fs/exists? (io/file (:envs STACK)))
      (throw (Exception. "."))
      (:envs STACK))
    (catch Exception _err
      (hpr (pre "File envs.json for aws sam not found.. Exiting!\n
Check https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-using-invoke.html#serverless-sam-cli-using-invoke-environment-file"))
      (System/exit 1))))

(def IMAGE_CORDS
  (or (:image DOCKER)
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

            (when-not (fs/exists? (io/file host))
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

(def INFRA (:infra OPTIONS))
(def RUNTIME (:runtime OPTIONS))
(def *RUNTIME_NAME* (:name RUNTIME))
(def NATIVE_IMAGE_ARGS (if-let [args (seq (:native-image-args RUNTIME))]
                         (s/join " " args)
                         nil))
(def INFRA_AWS_PROFILE (:profile INFRA))
(def AWS_PROFILE (or (:profile INFRA) "default"))
(def BUCKET_PREFIX (:bucket-prefix INFRA))
(def BUCKET_NAME (:bucket-name INFRA))
(def REGION_FROM_INFRA (:region INFRA))

(defn override-runtime!
  [new-runtime]
  (when new-runtime
    (let [runtime (keyword (s/replace new-runtime #":" ""))]
      (if-not (contains? AVAILABLE_RUNTIMES runtime)
        (do
          (hpr (pre "Runtime override from") (accent *RUNTIME_NAME*) (pre "to") (accent runtime) (pre "is not possible!"))
          (hpr (pre "Runtime") (accent runtime) (pre "is not one of") (accent AVAILABLE_RUNTIMES)))
        (alter-var-root #'*RUNTIME_NAME* (constantly runtime))))))

(defn can-obtain-from-aws-profile?!
  [what]
  (let [result (csh/sh "aws" "configure" "get" what "--profile" AWS_PROFILE)]
    (if-not (= (:exit result) 0)
      (throw (ex-info (str (pre "AWS configuration check failed. Unable to get value from the profile: ") (accent AWS_PROFILE)
                           (when-not (s/blank? (:err result))
                             (str "\n\n" (pre (:err result))))
                           (pre "\nDid you run command: ") (accent "aws configure") (pre "?"))
                      {}))
      true)))

(def REGION
  (if REGION_FROM_INFRA
    REGION_FROM_INFRA
    (try
      (can-obtain-from-aws-profile?! "region")
      (s/trim (:out (csh/sh "aws" "configure" "get" "region" "--profile" AWS_PROFILE)))
      (catch Exception e
        (hpr (ex-message e))))))

(def DEFAULT_LAMBDA_NAME (:default-lambda STACK))
(def RUNTIME_VERSION (:version RUNTIME))
(def ENTRYPOINT (:entrypoint (:runtime OPTIONS)))
(def OUTPUT_JAR_PATH ".holy-lambda/build/output.jar")
(def OUTPUT_JAR_PATH_WITH_AGENT ".holy-lambda/build/output-agent.jar")
(def HOLY_LAMBDA_DEPS_PATH ".holy-lambda/clojure/deps.edn")
(def STACK_NAME (:name STACK))
(def TEMPLATE_FILE (:template STACK))
(def REQUIRED_COMMANDS ["aws" "sam" "bb" "docker" "clojure" "zip" "id" "clj-kondo" "bash"])
(def CAPABILITIES (if-let [caps (seq (:capabilities STACK))]
                    caps
                    nil))
(def MODIFIED_TEMPLATE_FILE "template-modified.yml")
(def PACKAGED_TEMPLATE_FILE "packaged.yml")
(def BABASHKA_RUNTIME_LAYER_FILE ".holy-lambda/babashka-runtime/template.yml")
(def SELF_MANAGE_LAYERS? (:self-manage-layers? RUNTIME))
(def NATIVE_CONFIGURATIONS_PATH "resources/native-configuration")
(def NATIVE_CONFIGURATIONS_FILTER_REFLECTIONS_FILE_PATH "resources/native-configuration/reflections-filters.json")
(def NATIVE_CONFIGURATIONS_FILTER_REFLECTIONS_CONTENT
"{
  \"rules\": [
      {\"excludeClasses\": \"clojure.**\"},
      {\"includeClasses\": \"clojure.lang.Reflector\"}
  ]
}")
(def NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH "resources/native-configuration/resource-config.json")
(def BOOTSTRAP_FILE (:bootstrap-file RUNTIME))
(def NATIVE_DEPS_PATH (:native-deps RUNTIME))
(def BABASHKA_LAYER_INSTANCE (str BUCKET_NAME "-hlbbri-" (s/replace RUNTIME_VERSION #"\." "-")))
(def OFFICIAL_BABASHKA_LAYER_ARN "arn:aws:serverlessrepo:eu-central-1:443526418261:applications/holy-lambda-babashka-runtime")

;; VALIDATE IF AWS is configured properly
(if-not (command-exists? "aws")
  (do (hpr (accent "aws") (pre "command does not exists. Did you install AWS command line application?"))
      (System/exit 1))
  (try
    (can-obtain-from-aws-profile?! "aws_access_key_id")
    (can-obtain-from-aws-profile?! "aws_secret_access_key")
    (catch Exception e
      (hpr (ex-message e))
      (System/exit 1))))

(defn exit-if-not-synced!
  []
  (when-not (fs/exists? (io/file ".holy-lambda/clojure"))
    (hpr (pre "Project has not been synced yet. Run") (accent "stack:sync") (pre "before running this command!"))
    (System/exit 1)))

(defn aws-command-output->str
  [output]
  (str (pre "AWS command output:") "\n------------------------------------------\n" (s/trim output)
       "\n------------------------------------------"))

(defn -create-bucket
  [& [bucket throw?]]
  (let [bucket (or bucket BUCKET_NAME)
        result (csh/sh "aws" "s3" "mb" (str "s3://" bucket)
                       "--profile" AWS_PROFILE
                       "--region" REGION)]
    (if (not= (:exit result) 0)
      (do (hpr (pre "Unable to create a bucket")
               (str (accent bucket) "."))
          (hpr (aws-command-output->str (:err result)))
          (hpr (pre "Resolve the error then run the command once again if you have to."))
          (if throw?
            (throw (Exception. ""))
            (System/exit 1)))
      (hpr (prs "Bucket") (accent bucket) (prs "has been succesfully created!")))))

(defn -remove-bucket
  [& [bucket]]
  (shsp "aws" "s3" "rb"
        "--force" (str "s3://" (or bucket BUCKET_NAME))
        "--profile" AWS_PROFILE
        "--region" REGION))

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
  (fs/exists? (io/file AWS_DIR)))

(defn edn->pp-sedn
  [edn]
  (with-out-str (pprint/pprint edn)))

(defn map->parameters-inline
  [m]
  (s/join " " (mapv (fn [[k v]]
                      (str "ParameterKey=" (if (keyword? k) (name k) k) ",ParameterValue=" v))
                    m)))

(defn buckets
  []
  (set (mapv (fn [b] (some-> (re-find BUCKET_IN_LS_REGEX b) second))
             (s/split (shs "aws" "--profile" AWS_PROFILE "--region" REGION "s3" "ls")
                      #"\n"))))

(defn bucket-exists?
  [& [bucket-name]]
  (contains? (buckets) (or bucket-name BUCKET_NAME)))

(defn parameters--java
  []
  {"CodeUri"        OUTPUT_JAR_PATH
   "Runtime"        "java8"
   "Entrypoint"     ENTRYPOINT})

(defn parameters--babashka
  []
  {"CodeUri"        "src"
   "Runtime"        "provided"
   "Entrypoint"     ENTRYPOINT})

(defn parameters--native
  []
  {"CodeUri"        ".holy-lambda/build/latest.zip"
   "Runtime"        "provided"
   "Entrypoint"     ENTRYPOINT})

(defn -parameters
  []
  (case *RUNTIME_NAME*
    :java     (parameters--java)
    :babashka (parameters--babashka)
    :native   (parameters--native)))

(defn parameters
  [& [opt]]
  ((if (= opt :toml)
     (throw (ex-info ":toml not supported for now!" {}))
    ;; map->parameters-toml
    map->parameters-inline)
   (-parameters)))

(defn docker:run
  "     \033[0;31m>\033[0m Run command in \033[0;31mfierycod/graalvm-native-image\033[0m docker context\n\n----------------------------------------------------------------\n"
  [command]
  (apply shell
         (concat
          ["docker run --rm"
           "-e" "AWS_CREDENTIAL_PROFILES_FILE=/project/.aws/credentials"
           "-e" "AWS_CONFIG_FILE=/project/.aws/config"
           "-v" (str (.getAbsolutePath (io/file "")) ":/project")
           "-v" (str AWS_DIR ":" "/project/.aws:ro")]
          (flatten (mapv (fn [path] ["-v" path]) DOCKER_VOLUMES))
          ["--user" USER_GID
           "-it" IMAGE_CORDS
           "/bin/bash" "-c" command]))
  (shell "rm -Rf .aws"))

(defn deps-sync--babashka
  []
  (when (= *RUNTIME_NAME* :babashka)
    (when-not (empty? (:pods (:runtime OPTIONS)))
      (hpr "Babashka pods found! Syncing" (str (accent "babashka pods") ".") "Pods should be distributed via a layer which points to" (accent ".holy-lambda/pods"))
      (docker:run "download_pods")
      (when (fs/exists? (io/file ".holy-lambda/.babashka"))
        (shell "rm -Rf .holy-lambda/pods")
        (shell "mkdir -p .holy-lambda/pods")
        (shell "cp -R .holy-lambda/.babashka .holy-lambda/pods/")))))

(def -babashka-runtime-layer-template
"AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: >
  Babashka runtime as an AWS::Serverless::Application

Resources:
  HolyLambdaBabashkaRuntime:
    Type: AWS::Serverless::Application
    Properties:
      Location:
        ApplicationId: arn:aws:serverlessrepo:eu-central-1:443526418261:applications/holy-lambda-babashka-runtime
        SemanticVersion: <SEMANTIC_VERSION>")

(defn babashka-runtime-layer-template
  []
  (s/replace -babashka-runtime-layer-template #"<SEMANTIC_VERSION>" RUNTIME_VERSION))

(def tasks-deps-edn
  {:mvn/local-repo ".holy-lambda/.m2"
   :aliases {:deps {:replace-deps {'org.clojure/tools.deps.alpha {:mvn/version "0.11.910"}
                                   'org.slf4j/slf4j-nop {:mvn/version "1.7.25"}}
                    :ns-default 'clojure.tools.cli.api}

             :test {:extra-paths ["test"]}

             :uberjar {:replace-deps {'com.github.seancorfield/depstar {:mvn/version "2.0.216"}}
                       :exec-fn 'hf.depstar/uberjar
                       :exec-args {}}

             ;; build a jar (library):
             :jar {:replace-deps {'com.github.seancorfield/depstar {:mvn/version "2.0.216"}}
                   :exec-fn 'hf.depstar/jar
                   :exec-args {}}

             ;; generic depstar alias, use with jar or uberjar function name:
             :depstar {:replace-deps {'com.github.seancorfield/depstar {:mvn/version "2.0.216"}}
                       :ns-default 'hf.depstar
                       :exec-args {}}}})

(defn deps-sync--deps
  []
  (stat-file "deps.edn")
  (hpr "Syncing project and holy-lambda" (accent "deps.edn"))
  (docker:run (str "deps -A:" CLJ_ALIAS "depstar -P && deps -P"))
  (deps-sync--babashka))

(defn cloudformation-description
  [& [silent?]]
  (let [cloudformation-string (shs "aws" "cloudformation" "describe-stacks" "--region" REGION)
        cloudformation (if (s/blank? cloudformation-string)
                         (if silent?
                           nil
                           (do (hpr (pre "Unable to get information about stacks. Use AWS UI to get proper ARN for layer"))
                               (System/exit 1)))
                         (try
                           (json/parse-string cloudformation-string true)
                           (catch Exception err
                             (if silent?
                               nil
                               (do
                                 (hpr (pre "Unable to parse information about stacks."))
                                 (println err)
                                 (System/exit 1))))))]
    cloudformation))

(defn stack->info
  [stack]
  (let [tags (group-by :Key (:Tags stack))
        get-val-by (fn [sel] (:Value (first (get tags sel))))]
    {:version       (get-val-by "serverlessrepo:semanticVersion")
     :arn           (:OutputValue (first (:Outputs stack)))
     :app-id        (get-val-by "serverlessrepo:applicationId")
     :status        (:StackStatus stack)
     :stack-id      (:StackId stack)
     :stack-name    (:StackName stack)
     :capabilities  (:Capabilities stack)
     :description   (:Description stack)
     :last-updated  (or (:LastUpdatedTime stack) :not-updated)
     :created-at    (:CreationTime stack)
     :parent-id     (:ParentId stack)}))

(defn app-id->app-layers
  [app-id]
  (if-let [stacks (seq (:Stacks (cloudformation-description)))]
    (let [layers (->> stacks
                      (keep
                       (fn [stack]
                         (let [info (stack->info stack)]
                           (when (= app-id (:app-id info))
                             info))))
                      vec)
          layers-id-set (set (mapv :stack-id layers))
          groupped-stacks  (->> stacks
                                (filter (complement (fn [s] (contains? layers-id-set (:StackId s)))))
                                (mapv stack->info)
                                (group-by :stack-id))
          layers-by-parents (group-by :parent-id layers)
          app<->layers (mapv (fn [[parent-stack-id stack]]
                               (let [parent-of-stack (first (get groupped-stacks parent-stack-id))]
                                 (assoc parent-of-stack :child stack)))
                             layers-by-parents)]
      app<->layers)
    (hpr "No stacks found in cloudformation definitions.")))

(defn babashka-layer
  []
  (let [app<->layers (app-id->app-layers OFFICIAL_BABASHKA_LAYER_ARN)]
    (first (:child (first app<->layers)))))

(defn publish-babashka-layer
  []
  (io/make-parents BABASHKA_RUNTIME_LAYER_FILE)
  (spit BABASHKA_RUNTIME_LAYER_FILE (babashka-runtime-layer-template))

  (when-not (bucket-exists? BABASHKA_LAYER_INSTANCE)
    (-create-bucket BABASHKA_LAYER_INSTANCE))

  (apply shell
         "sam deploy"
         "--template-file"  BABASHKA_RUNTIME_LAYER_FILE
         "--stack-name"     BABASHKA_LAYER_INSTANCE
         "--profile"        AWS_PROFILE
         "--s3-bucket"      BABASHKA_LAYER_INSTANCE
         "--no-confirm-changeset"
         "--capabilities"   ["CAPABILITY_IAM" "CAPABILITY_AUTO_EXPAND"])

  (hpr "Waiting 5 seconds for deployment to propagate...")
  (Thread/sleep 5000)
  (hpr "Checking the ARN of published layer. This might take a while..")
  (hpr (prs "Your ARN for babashka runtime layer is:") (accent (:arn (babashka-layer))))
  (hpr "You should add the provided ARN as a property of a Function in template.yml!\n
---------------" (accent "template.yml") "------------------\n
      Resources:
        ExampleLambdaFunction:
          Type: AWS::Serverless::Function
          Properties:
            Handler: example.core.ExampleLambda"
       (prs "
            Layers:
              - PLEASE_ADD_THE_ARN_OF_LAYER_HERE")
       "
            Events:
              HelloEvent:
                Type: Api
                Properties:
                  Path: /
                  Method: get\n
---------------------------------------------"))

(defn runtime-sync-hook--babashka
  []
  (hpr (str "Cloning Clojure deps for " (accent "babashka") ".") "If you're using some extra dependencies provide a layer with CodeUri:" (accent ".holy-lambda/bb-clj-deps"))
  (shell "bash -c \"mkdir -p .holy-lambda/bb-clj-deps && cp -R .holy-lambda/.m2 .holy-lambda/bb-clj-deps/\"")

  (when-not SELF_MANAGE_LAYERS?
    (if-let [bb-layer (babashka-layer)]
      (if (= (:version bb-layer) RUNTIME_VERSION)
        (hpr "Babashka runtime layer exists. Your layer ARN is:" (accent (:arn bb-layer)) "(deployment skipped)")
        (do (hpr "Version from bb.edn does not match deployed version of the runtime")
            (hpr "Updating deployed version from" (accent (:version bb-layer)) "to" (accent RUNTIME_VERSION))
            (publish-babashka-layer)))
      (do
        (hpr "Babashka runtime needs a special layer for both local invocations and deployments.")
        (hpr "Layer is published here:" "https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime")
        (println "")
        (hpr (str "Layer is not published! Trying to deploy layer:\n\n" (babashka-runtime-layer-template) "\n"))
        (publish-babashka-layer)))))

(defn runtime-sync-hook
  []
  (case *RUNTIME_NAME*
    :babashka (runtime-sync-hook--babashka)
    nil))

(defn stack:sync
  "     \033[0;31m>\033[0m Syncs project & dependencies from either:
       \t\t        - \033[0;31m<Clojure>\033[0m  project.clj
       \t\t        - \033[0;31m<Clojure>\033[0m  deps.edn
       \t\t        - \033[0;31m<Babashka>\033[0m bb.edn:runtime:pods"
  []
  (print-task "stack:sync")
  (when-not (fs/exists? (io/file ".holy-lambda/clojure/deps.edn"))
    (hpr "Project not synced yet. Syncing with docker image!")
    (let [cid (gensym "holy-lambda")]
      (shs "docker" "create" "--user" USER_GID "-ti" "--name" (str cid)  IMAGE_CORDS "bash")
      (shs "docker" "cp" (str cid ":/project/.holy-lambda") ".")
      (shs "docker" "rm" "-f" (str cid))))

  (when-not (fs/exists? (io/file ".holy-lambda"))
    (hpr (pre "Unable to sync docker image content with") (accent ".holy-lambda") (pre "project directory!")))

  (when-not (fs/exists? (io/file ".holy-lambda/clojure"))
    (hpr (pre "Project did not sync properly. Remove .holy-lambda directory and run") (accent "stack:sync")))

  ;; Correct holy-lambda deps.edn
  (spit HOLY_LAMBDA_DEPS_PATH (edn->pp-sedn tasks-deps-edn))

  ;; Sync
  (deps-sync--deps)

  ;; Runtime postprocess hook
  (runtime-sync-hook)

  (hpr "Sync completed!"))

(defn stack-files-check--native
  []
  (when-not (fs/exists? (io/file ".holy-lambda/build/latest.zip"))
    (hpr (pre "No") (accent ".holy-lambda/build/latest.zip") (pre "found! Run") (accent "native:executable"))
    (System/exit 1)))

(defn stack-files-check--java
  []
  (when-not (fs/exists? (io/file OUTPUT_JAR_PATH))
    (hpr (pre "No") (accent OUTPUT_JAR_PATH) (pre "found! Run") (accent "stack:compile"))
    (System/exit 1)))

(defn stack-files-check
  [& [check]]
  (when-not (fs/exists? (io/file ".holy-lambda"))
    (hpr (pre "No") (accent ".holy-lambda") (pre "directory! Run") (accent "stack:sync"))
    (System/exit 1))

  (case (or check *RUNTIME_NAME*)
    :java   (stack-files-check--java)
    :native (do
              (stack-files-check--java)
              (stack-files-check--native))
    :babashka nil
    nil))

(defn build-stale?
  []
  (and
   (not= *RUNTIME_NAME* :babashka)
   (boolean (seq (fs/modified-since OUTPUT_JAR_PATH (fs/glob "src" "**/**.{clj,cljc,cljs}"))))))

(defn stack:api
  "     \033[0;31m>\033[0m Runs local api (check sam local start-api):
       \t\t        - \033[0;31m:debug\033[0m       - run api in \033[0;31mdebug mode\033[0m
       \t\t        - \033[0;31m:port\033[0m        - local port number to listen to
       \t\t        - \033[0;31m:static-dir\033[0m  - assets which should be presented at \033[0;31m/\033[0m
       \t\t        - \033[0;31m:envs-file\033[0m   - path to \033[0;31menvs file\033[0m
       \t\t        - \033[0;31m:runtime\033[0m     - overrides \033[0;31m:runtime:name\033[0m and run Lambda in specified runtime
       \t\t        - \033[0;31m:params\033[0m      - map of parameters to override in AWS SAM"
  [& args]
  (print-task "stack:api")
  (exit-if-not-synced!)
  (let [{:keys [static-dir debug envs-file port params runtime]} (norm-args args)]
    (override-runtime! runtime)
    (stack-files-check)
    (when (build-stale?)
      (hpr (prw "Build is stale. Consider recompilation via") (accent "stack:compile")))

    (shell (str "sam local start-api"
                " --parameter-overrides "    (if-not params
                                               (parameters)
                                               (str (parameters) " " (map->parameters-inline (edn/read-string params))))
                " --template "               TEMPLATE_FILE
                " --profile "                AWS_PROFILE
                " -p "                       (or port 3000)
                (when static-dir " -s ")     static-dir
                " --warm-containers LAZY"
                " -n "                       (or envs-file DEFAULT_ENVS_FILE)
                " --layer-cache-basedir "    LAYER_CACHE_DIRECTORY
                (when debug " --debug")))))

(defn native:conf
  "     \033[0;31m>\033[0m Provides native configurations for the application
       \t\t        - \033[0;31m:runtime\033[0m     - overrides \033[0;31m:runtime:name\033[0m and run Lambda in specified runtime "
  [& args]
  (print-task "native:conf")
  (exit-if-not-synced!)
  (let [{:keys [runtime]} (norm-args args)]
    (override-runtime! runtime)

    (when-not (= *RUNTIME_NAME* :native)
      (hpr (pre "Command") (accent "native:conf") (pre "supports only") (accent ":native") (pre "runtime!"))
      (System/exit 1))

    (stack-files-check :default)

    (when-not (fs/exists? (io/file NATIVE_CONFIGURATIONS_FILTER_REFLECTIONS_FILE_PATH))
      (spit NATIVE_CONFIGURATIONS_FILTER_REFLECTIONS_FILE_PATH NATIVE_CONFIGURATIONS_FILTER_REFLECTIONS_CONTENT))

    (hpr "Compiling with agent support")
    (shell "rm -Rf .cpcache .holy-lambda/build/output-agent.jar")
    (docker:run (str "USE_AGENT_CONTEXT=true clojure -X:uberjar :aliases '" (str [CLJ_ALIAS_KEY]) "' :aot '[\"" (str ENTRYPOINT) "\"]' " ":jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar " OUTPUT_JAR_PATH_WITH_AGENT " :main-class " (str ENTRYPOINT)))

    (hpr "Generating native-configurations")
    (docker:run (str "java -agentlib:native-image-agent="
                     "config-output-dir=" NATIVE_CONFIGURATIONS_PATH ","
                     "caller-filter-file=" NATIVE_CONFIGURATIONS_FILTER_REFLECTIONS_FILE_PATH
                     " "
                     "-Dexecutor=native-agent -jar " OUTPUT_JAR_PATH_WITH_AGENT))
    (if-not (fs/exists? (io/file NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH))
      (hpr (pre "Native configurations generation failed"))
      (let [resource-config (json/parse-string (slurp (io/file NATIVE_CONFIGURATIONS_RESOURCE_CONFIG_FILE_PATH)))]
        (hpr "Removing unecessary entries in resource-config.json")
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
               {:pretty true}))))))

(def -bootstrap-file
"#!/bin/sh

set -e

./output")

(defn bootstrap-file
  []
  (or (when (fs/exists? (io/file BOOTSTRAP_FILE))
       (slurp BOOTSTRAP_FILE))
      -bootstrap-file))

(defn native:executable
  "     \033[0;31m>\033[0m Provides native executable of the application
  \t\t        - \033[0;31m:runtime\033[0m     - overrides \033[0;31m:runtime:name\033[0m and run Lambda in specified runtime
\n----------------------------------------------------------------\n"
  [& args]
  (print-task "native:executable")
  (exit-if-not-synced!)
  (let [{:keys [runtime]} (norm-args args)]
    (override-runtime! runtime)
    (when-not (= *RUNTIME_NAME* :native)
      (hpr (pre "Command") (accent "native:executable") (pre "supports only") (accent ":native") (pre "runtime!"))
      (System/exit 1))

    (stack-files-check :java)

    (when (build-stale?)
      (hpr (prw "Build is stale. Consider recompilation via") (accent "stack:compile")))

    (when-not (fs/exists? (io/file NATIVE_CONFIGURATIONS_PATH))
      (hpr (prw "No native configurations has been generated. Native image build may fail. Run") (accent "native:conf") (prw "to generate native configurations.")))

    ;; Copy then build
    (shell-no-exit "bash -c \"[ -d resources/native-configuration ] && cp -rf resources/native-configuration .holy-lambda/build/\"")

    (docker:run (str "cd .holy-lambda/build/ && native-image -jar output.jar -H:ConfigurationFileDirectories=native-configuration "
                     "-H:+AllowIncompleteClasspath"
                     (when NATIVE_IMAGE_ARGS
                       (str " " NATIVE_IMAGE_ARGS))))

    (if-not (fs/exists? (io/file ".holy-lambda/build/output"))
      (hpr (pre "Native image failed to create executable. Fix your build! Skipping next steps"))
      (do
        (spit ".holy-lambda/build/bootstrap" (bootstrap-file))
        (when (and NATIVE_DEPS_PATH (fs/exists? (io/file NATIVE_DEPS_PATH)))
          (hpr "Copying" (accent ":runtime:native-deps"))
          (shell (str "cp -R " NATIVE_DEPS_PATH " .holy-lambda/build/")))
        (hpr "Bundling artifacts...")
        (shell "bash -c \"cd .holy-lambda/build && chmod +x bootstrap\"" )
        (shell "bash -c \"cd .holy-lambda/build && rm -Rf output-agent.jar native-configuration resources/native-configuration resources/native-agents-payloads output.build_artifacts.txt\"")
        (shell "bash -c \"cd .holy-lambda/build && zip -r latest.zip . -x 'output.jar'\"")))))

(defn modify-template
  []
  (let [buffer (slurp TEMPLATE_FILE)
        {:strs [CodeUri Runtime]} (-parameters)]

    (when-not (re-find #"<HOLY_LAMBDA_CODE_URI>" buffer)
      (hpr (pre "<HOLY_LAMBDA_CODE_URI> definition should be available. Check related issue https://github.com/aws/aws-sam-cli/issues/2835"))
      (System/exit 1))

    (when-not (re-find #"<HOLY_LAMBDA_RUNTIME>" buffer)
      (hpr (pre "<HOLY_LAMBDA_RUNTIME> definition should be available. Check related issue https://github.com/aws/aws-sam-cli/issues/2835"))
      (System/exit 1))

    (spit MODIFIED_TEMPLATE_FILE
          (-> buffer
              (s/replace #"<HOLY_LAMBDA_CODE_URI>" CodeUri)
              (s/replace #"<HOLY_LAMBDA_RUNTIME>" Runtime)
              (s/replace #"\!Ref CodeUri" CodeUri)))))

(defn bucket:create
  "     \033[0;31m>\033[0m Creates a s3 stack bucket or the one specified by \033[0;31m:name\033[0m"
  [& args]
  (print-task "bucket:create")
  (let [{:keys [name]} (norm-args args)
        bucket-name (or name BUCKET_NAME)]
    (if (bucket-exists? bucket-name)
      (do
        (hpr (prs "Bucket") (accent bucket-name) (prs "already exists!"))
        (hpr (prw "If you removed the bucket then note that sometimes bucket is not immediately appear to be removed and is still listed in AWS resources."))
        (when-not name
          (hpr (prw "In such case change:")
               (str (accent ":infra:bucket-name") "!"))))
      (do (hpr (prs "Creating a bucket") (accent bucket-name))
          (-create-bucket bucket-name)))))

(defn check-n-create-bucket
  []
  (when-not (bucket-exists?)
    (hpr (prw "Bucket") (accent BUCKET_NAME) "does not exists. Creating one!")
    (bucket:create)))

(defn stack:pack
  "     \033[0;31m>\033[0m Packs \033[0;31mCloudformation\033[0m stack
   \t\t        - \033[0;31m:runtime\033[0m     - overrides \033[0;31m:runtime:name\033[0m and run Lambda in specified runtime "
  [& args]
  (print-task "stack:pack")
  (let [{:keys [runtime]} (norm-args args)]
    (override-runtime! runtime)
    (exit-if-not-synced!)
    (stack-files-check)
    ;; Check https://github.com/aws/aws-sam-cli/issues/2835
    ;; https://github.com/aws/aws-sam-cli/issues/2836
    (check-n-create-bucket)
    (modify-template)
    (shell "sam" "package"
           "--template-file"        MODIFIED_TEMPLATE_FILE
           "--output-template-file" PACKAGED_TEMPLATE_FILE
           "--profile"              AWS_PROFILE
           "--s3-bucket"            BUCKET_NAME
           "--s3-prefix"            BUCKET_PREFIX
           "--region"               REGION)))

(defn bucket:remove
  "     \033[0;31m>\033[0m Removes a s3 stack bucket or the one specified by \033[0;31m:name\033[0m\n\n----------------------------------------------------------------\n"
  [& args]
  (print-task "bucket:remove")
  (let [{:keys [name]} (norm-args args)
        bucket-name (or name BUCKET_NAME)]
    (if-not (bucket-exists? bucket-name)
      (hpr (pre "Bucket") (accent bucket-name) (pre "does not exists! Nothing to remove!"))
      (do (hpr (prs "Removing a bucket") (accent bucket-name))
          (-remove-bucket bucket-name)))))

(defn stack:deploy
  "     \033[0;31m>\033[0m Deploys \033[0;31mCloudformation\033[0m stack
  \t\t        - \033[0;31m:guided\033[0m      - guide the deployment
  \t\t        - \033[0;31m:dry\033[0m         - execute changeset?
  \t\t        - \033[0;31m:params\033[0m      - map of parameters to override in AWS SAM
  \t\t        - \033[0;31m:runtime\033[0m     - overrides \033[0;31m:runtime:name\033[0m and run Lambda in specified runtime "
  [& args]
  (print-task "stack:deploy")
  (let [{:keys [guided dry params runtime]} (norm-args args)]
    (override-runtime! runtime)
    (exit-if-not-synced!)
    (if-not (fs/exists? (io/file PACKAGED_TEMPLATE_FILE))
      (hpr (pre "No") (accent PACKAGED_TEMPLATE_FILE) (pre "found. Run") (accent "stack:pack"))
      (do
        (check-n-create-bucket)
        (apply shell "sam" "deploy"
               "--template-file" PACKAGED_TEMPLATE_FILE
               "--stack-name"    STACK_NAME
               "--profile"       AWS_PROFILE
               "--region"        REGION
               (when dry "--no-execute-changeset")
               (when guided "--guided")
               "--parameter-overrides" (if-not params
                                         (parameters)
                                         (str (parameters) " " (map->parameters-inline (edn/read-string params))))
               (when CAPABILITIES "--capabilities") CAPABILITIES)))))

(defn stack:compile
  "     \033[0;31m>\033[0m Compiles sources if necessary
  \t\t        - \033[0;31m:force\033[0m      - force compilation even if sources did not change"
  [& args]
  (print-task "stack:compile")
  (exit-if-not-synced!)
  (let [{:keys [force]} (norm-args args)])
  (when (= *RUNTIME_NAME* :babashka)
    (hpr "Nothing to compile. Sources are provided as is to" (accent "babashka") "runtime")
    (System/exit 0))
  (when (and (not (build-stale?)) (not force))
    (hpr "Nothing to compile. Sources did not change!")
    (System/exit 0))
  (shell "rm -Rf .cpcache .holy-lambda/build")
  (docker:run (str "clojure -X:uberjar :aliases '" (str [CLJ_ALIAS_KEY]) "' :aot '[\"" (str ENTRYPOINT) "\"]' " ":jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar " OUTPUT_JAR_PATH " :main-class " (str ENTRYPOINT))))

(defn stack:invoke
  "     \033[0;31m>\033[0m Invokes lambda fn (check sam local invoke --help):
       \t\t        - \033[0;31m:name\033[0m        - either \033[0;31m:name\033[0m or \033[0;31m:stack:default-lambda\033[0m
       \t\t        - \033[0;31m:event-file\033[0m  - path to \033[0;31mevent file\033[0m
       \t\t        - \033[0;31m:envs-file\033[0m   - path to \033[0;31menvs file\033[0m
       \t\t        - \033[0;31m:params\033[0m      - map of parameters to override in AWS SAM
       \t\t        - \033[0;31m:runtime\033[0m     - overrides \033[0;31m:runtime:name\033[0m and run Lambda in specified runtime
       \t\t        - \033[0;31m:debug\033[0m       - run invoke in \033[0;31mdebug mode\033[0m
       \t\t        - \033[0;31m:logs\033[0m        - logfile to runtime logs to"
  [& args]
  (print-task "stack:invoke")
  (let [{:keys [name event-file envs-file logs params debug runtime]} (norm-args args)]
    (override-runtime! runtime)
    (exit-if-not-synced!)
    (stack-files-check)
    (when (build-stale?)
      (hpr (prw "Build is stale. Consider recompilation via") (accent "stack:compile")))
    (shell "sam" "local" "invoke"    (or name DEFAULT_LAMBDA_NAME)
           "--parameter-overrides"   (if-not params
                                       (parameters)
                                       (str (parameters) " " (map->parameters-inline (edn/read-string params))))
           "--profile"               AWS_PROFILE
           (when debug "--debug")
           (when logs "-l")          logs
           (when event-file "-e")    event-file
           "-n" (or envs-file DEFAULT_ENVS_FILE))))

(defn mvn-local-test
  [file]
  (if (not= (:mvn/local-repo (edn/read-string (slurp (io/file file)))) ".holy-lambda/.m2")
    (hpr (pre "property") (accent ":mvn/local-repo") (pre "in file") (accent file) (pre "should be set to") (accent ".holy-lambda/.m2"))
    (hpr (prs "property") (accent ":mvn/local-repo") (prs "in file") (accent file) (prs "is correct"))))

(defn stack:doctor
  "     \033[0;31m>\033[0m Diagnoses common issues of holy-lambda stack"
  []
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
    (hpr " Runtime:                 " (accent *RUNTIME_NAME*))
    (hpr " Runtime entrypoint:      " (accent ENTRYPOINT))
    (hpr " Stack name:              " (accent STACK_NAME))
    (hpr " S3 Bucket name:          " (accent BUCKET_NAME))
    (hpr " S3 Bucket prefix:        " (accent BUCKET_PREFIX))
    (hpr " S3 Bucket exists?:       " (accent (bucket-exists?)))
    (hpr "---------------------------------------\n"))

  (when-not (fs/exists? (io/file AWS_DIR))
    (hpr (pre "$HOME/.aws does not exists. Did you run") (accent "aws configure")))

  (read-string (slurp (io/file "deps.edn")))

  (if-not (contains? AVAILABLE_RUNTIMES *RUNTIME_NAME*)
    (do
      (hpr (str (pre ":runtime ") (accent *RUNTIME_NAME*) (pre " is not supported!")))
      (hpr (str "Choose one of supported build tools: " AVAILABLE_RUNTIMES)))
    (hpr (prs ":runtime looks good")))

  (if-not ENTRYPOINT
    (hpr (pre ":runtime:entrypoint is required!"))
    (hpr (prs ":runtime:entrypoint looks good")))

  (if-not CAPABILITIES
    (hpr (pre ":stack:capabilities is required!"))
    (hpr (prs ":stack:capabilities looks good")))

  (when-not INFRA_AWS_PROFILE
    (hpr (prw ":infra:profile which should point to AWS Profile is not declared, therefore") (accent "default") (prw "profile will be used instead")))

  (when (and (not= *RUNTIME_NAME* :native) BOOTSTRAP_FILE)
    (hpr (prw ":runtime:bootstrap-file is supported only for") (accent ":native") (prw "runtime")))

  (when (and (= *RUNTIME_NAME* :native) BOOTSTRAP_FILE (not (fs/exists? (io/file BOOTSTRAP_FILE))))
    (hpr (prw ":runtime:bootstrap-file does not exists. Default bootstrap file for") (accent ":native") (prw "runtime will be used!")))

  (when (and (not= *RUNTIME_NAME* :native) NATIVE_DEPS_PATH)
    (hpr (prw ":runtime:native-deps is supported only for") (accent ":native") (prw "runtime")))

  (when (and (= *RUNTIME_NAME* :native)
             NATIVE_DEPS_PATH
             (not (fs/exists? (io/file NATIVE_DEPS_PATH))))
    (hpr (prw ":runtime:native-deps folder does not exists") (accent ":native:executable") (prw "will not include any extra deps!")))

  (when (and RUNTIME_VERSION (not= *RUNTIME_NAME* :babashka))
    (hpr (prw ":runtime:version is supported only for") (accent ":babashka") (prw "runtime")))

  (when (and (:pods RUNTIME) (not= *RUNTIME_NAME* :babashka))
    (hpr (prw ":runtime:pods are supported only for") (accent ":babashka") (prw "runtime")))

  (when (and NATIVE_IMAGE_ARGS (not= *RUNTIME_NAME* :native))
    (hpr (prw ":runtime:native-image-args are supported only for") (accent ":native") (prw "runtime")))

  (if-not STACK_NAME
    (hpr (pre ":stack:name is required!"))
    (hpr (prs ":stack:name looks good")))

  (mvn-local-test "deps.edn")
  (mvn-local-test "bb.edn")

  (if (fs/exists? (io/file HOLY_LAMBDA_DEPS_PATH))
    (hpr (prs "Syncing stack is not required"))
    (hpr (pre "Stack is not synced! Run:") (accent "stack:sync")))

  (if-not (contains? AVAILABLE_REGIONS REGION)
    (do
      (hpr (str (pre "Region ") (accent REGION) (pre " is not supported!")))
      (hpr (str "Choose one of supported regions:\n" (with-out-str (pprint/pprint AVAILABLE_REGIONS)))))
    (hpr (prs ":infra:region definition looks good")))

  (if (s/includes? BUCKET_PREFIX "_")
    (hpr (pre ":infra:bucket-prefix should not contain any of _ characters"))
    (hpr (prs ":infra:bucket-prefix looks good")))

  (if-not TEMPLATE_FILE
    (hpr (pre ":stack:template is required!"))
    (hpr (prs ":stack:template looks good")))

  (if (s/includes? BUCKET_NAME "_")
    (hpr (pre ":infra:bucket-name should not contain any of _ characters"))
    (hpr (str (prs ":infra:bucket-name looks good")
              (when-not (bucket-exists?)
                (str (prs ", but ") (accent BUCKET_NAME) (prw " does not exists (use bb :bucket:create)"))))))

  (if-let [cmds-not-found (seq (filter (comp not command-exists?) REQUIRED_COMMANDS))]
    (hpr (str (pre (str "Commands " cmds-not-found " not found. Install all then run: ")) (underline "bb doctor")))
    (do
      (hpr (prs "Required commands") (accent (str REQUIRED_COMMANDS)) (prs "installed!"))
      (println)
      (stat-file TEMPLATE_FILE)
      (hpr "Validating" (accent TEMPLATE_FILE))
      (shell "sam validate"))))

(defn stack:logs
  "     \033[0;31m>\033[0m Possible arguments (check sam logs --help):
       \t\t        - \033[0;31m:name\033[0m        - either \033[0;31m:name\033[0m or \033[0;31m:stack:default-lambda\033[0m
       \t\t        - \033[0;31m:e\033[0m           - fetch logs up to this time
       \t\t        - \033[0;31m:s\033[0m           - fetch logs starting at this time
       \t\t        - \033[0;31m:filter\033[0m      - find logs that match terms "
  [& args]
  (print-task "stack:logs")
  (let [{:keys [name tail s e filter]} (norm-args args)]
    (shell "sam" "logs"
           "--profile"  AWS_PROFILE
           "-n" (or name DEFAULT_LAMBDA_NAME)
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

(defn stack:version
  "     \033[0;31m>\033[0m Outputs holy-lambda babashka tasks version"
  []
  (print-task "stack:version")
  (hpr (str (prs "Current tasks version is: ") (accent TASKS_VERSION)))
  (when-not (local-tasks-match-remote?)
    (hpr "There is newer version of tasks on remote. Please update tasks :sha")))

(defn stack:purge
  "     \033[0;31m>\033[0m Purges build artifacts"
  []
  (print-task "stack:purge")
  (let [artifacts [".aws"
                   ".holy-lambda"
                   ".cpcache"
                   "node_modules"]]

    (hpr  (str (accent "Purging build artifacts:") "\n\n" (plist artifacts)))

    (doseq [art artifacts]
      (shell (str "rm -rf " art)))

    (hpr  (prs "Build artifacts purged"))))

(defn docker:build:ee
  "     \033[0;31m>\033[0m Builds local image for GraalVM EE "
  []
  (print-task "docker:build:ee")
  (hpr  (accent "Building GraalVM EE docker image"))
  (spit "Dockerfile.ee" (:body (curl/get "https://raw.githubusercontent.com/FieryCod/holy-lambda/master/docker/ee/Dockerfile")))
  (shell "docker build . -f Dockerfile.ee -t fierycod/graalvm-native-image:ee")
  (shell "rm -rf Dockerfile.ee"))

(defn stack:lint
  "     \033[0;31m>\033[0m Lints the project"
  []
  (print-task "stack:lint")
  (shell "clj-kondo --lint src:test"))

(defn stack:describe
  "     \033[0;31m>\033[0m Describes \033[0;31mCloudformation\033[0m stack"
  []
  (print-task "stack:describe")
  (shell "aws" "cloudformation" "describe-stacks"
         "--profile"    AWS_PROFILE
         "--region"     REGION
         "--stack-name" STACK_NAME))

(defn stack:destroy
  "     \033[0;31m>\033[0m Destroys \033[0;31mCloudformation\033[0m stack & removes bucket"
  []
  (print-task "stack:destroy")
  (hpr (prw "Automatic cloudformation") (accent "stack:destroy") (prw "operation might not be always successful."))
  (shell "aws" "cloudformation" "delete-stack"
         "--profile"    AWS_PROFILE
         "--region"     REGION
         "--stack-name" STACK_NAME)
  (bucket:remove)
  (hpr "Waiting 5 seconds for partial/complete cloudformation deletion status...")
  (Thread/sleep 5000)
  (hpr "If you see an error regarding not being able to describe not existent stack then destroy was successful!")
  (stack:describe))
