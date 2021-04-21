(gen-class)

(require
 '[clojure.java.shell :as shell]
 '[clojure.java.io :as io]
 '[clojure.string :as s])

(defn sh
  [& args]
  (println (apply shell/sh args)))

(def VERSION (s/trim (slurp (io/file "VERSION"))))

(def EXAMPLES
  ["hello-lambda"
   "sqs-example"])

(def VERSION_GROUPS #"([0-9]+)\.([0-9]+)\.([0-9]+)(?:-SNAPSHOT)?")
(def PROJECT_VERSION #"io\.github\.FieryCod\/holy-lambda\s*\"([0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?)\"")
(def TEMPLATE_PROJECT_VERSION #"holy-lambda\/lein-template\s*\"([0-9]+\.[0-9]+\.[0-9]+(?:-SNAPSHOT)?)\"")

(defn bump
  [?type ?version]
  (let [[_ major minor patch] (re-find VERSION_GROUPS ?version)
        major (if-not (= ?type :major) major (inc (Integer/parseInt major)))
        minor (if-not (= ?type :minor) minor (inc (Integer/parseInt minor)))
        patch (if-not (= ?type :patch) patch (Integer/parseInt patch))
        snapshot (if-not (= ?type :snapshot) "" "-SNAPSHOT")]
    (str major "." minor "." (if (= ?type :snapshot)
                               (inc (Integer/parseInt patch))
                               patch)
         snapshot)))

(defn deploy
  [{:keys [type] :or {type "patch"}}]
  (let [type (keyword type)
        new-version (bump type VERSION)]

    ;; Update all examples
    (doseq [example-path EXAMPLES
            :let [file (io/file "examples" example-path "project.clj")]]
      (spit (str (.getAbsolutePath file)) (s/replace (slurp file) PROJECT_VERSION (str "io.github.FieryCod/holy-lambda   \"" new-version "\""))))


    ;; Update project, README and VERSION
    (spit "project.clj" (s/replace (slurp "project.clj") PROJECT_VERSION (str "io.github.FieryCod/holy-lambda   \"" new-version "\"")))
    (spit "modules/holy-lambda-babashka-release/project.clj" (s/replace (slurp "modules/holy-lambda-babashka-release/project.clj") PROJECT_VERSION (str "io.github.FieryCod/holy-lambda   \"" new-version "\"")))
    (spit "README.md" (s/replace (slurp "README.md") PROJECT_VERSION (str "io.github.FieryCod/holy-lambda \"" new-version "\"")))
    (spit "VERSION" new-version)

    ;; Update dependant template
    (spit "modules/holy-lambda-template/project.clj" (s/replace (slurp "modules/holy-lambda-template/project.clj") TEMPLATE_PROJECT_VERSION (str "holy-lambda/lein-template   \"" new-version "\"")))
    (spit "modules/holy-lambda-template/resources/leiningen/new/holy_lambda/project.clj" (s/replace (slurp "modules/holy-lambda-template/resources/leiningen/new/holy_lambda/project.clj") PROJECT_VERSION (str "io.github.FieryCod/holy-lambda   \"" new-version "\"")))

    ;; Deploy dependant template
    (sh "bash" "-c" "cd modules/holy-lambda-template/ && lein install")
    (sh "bash" "-c" "cd modules/holy-lambda-template/ && lein deploy clojars")

    ;; Release new version of holy-lambda
    (sh "git" "add" ".")
    (sh "git" "commit" "-m" (str "[deployer] Release v" new-version))
    (sh "git" "tag" new-version)
    (sh "git" "push")
    (sh "git" "push" "origin" "--tags")
    (sh "lein" "install")
    (sh "lein" "deploy" "clojars")

    ;; Prepare for new development iteration
    (spit "VERSION" (bump :snapshot new-version))
    (spit "project.clj" (s/replace (slurp "project.clj") PROJECT_VERSION (str "io.github.FieryCod/holy-lambda   \"" (bump :snapshot new-version) "\"")))
    (spit "modules/holy-lambda-babashka-release/project.clj" (s/replace (slurp "modules/holy-lambda-babashka-release/project.clj") (bump :snapshot new-version) (str "io.github.FieryCod/holy-lambda   \"" new-version "\"")))
    (spit "modules/holy-lambda-template/project.clj" (s/replace (slurp "modules/holy-lambda-template/project.clj") TEMPLATE_PROJECT_VERSION (str "holy-lambda/lein-template   \"" (bump :snapshot new-version) "\"")))

    (sh "git" "add" ".")
    (sh "git" "commit" "-m" (str "[deployer] Prepare for next development iteration v" (bump :snapshot new-version)))
    (sh "git" "push"))
  (shutdown-agents))

(deploy {:type (first *command-line-args*)})
