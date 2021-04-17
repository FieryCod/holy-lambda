(gen-class)

(require
 '[clojure.java.io :as io]
 '[clojure.string :as s])

(def VERSION (s/trim (slurp (io/file "VERSION"))))

(def examples
  ["hello-lambda"
   "sqs-example"])

(def VERSION_GROUPS #"([0-9]+)\.([0-9]+)\.([0-9]+)(?:SNAPSHOT)?")
(def PROJECT_VERSION #"fierycod\/holy-lambda\s*\"([0-9]+\.[0-9]+\.[0-9]+(?:SNAPSHOT)?)\"")

(defn bump
  [?type ?version]
  (let [[_ major minor patch] (re-find VERSION_GROUPS ?version)
        major (if-not (= ?type :major) major (inc (Integer/parseInt major)))
        minor (if-not (= ?type :minor) minor (inc (Integer/parseInt minor)))
        patch (if-not (= ?type :patch) patch (inc (Integer/parseInt patch)))
        snapshot (if-not (= ?type :snapshot) "" "-SNAPSHOT")]
    (str major "." minor "." (if (= ?type :snapshot)
                               (inc patch)
                               patch)
         snapshot)))


(defn deploy
  [{:keys [type] :or {type "patch"}}]
  (let [type (keyword type)
        new-version (bump type VERSION)]
    (doseq [example-path examples
            :let [file (io/file "examples" example-path "project.clj")]]
      (spit (str (.getAbsolutePath file)) (s/replace (slurp file) PROJECT_VERSION (str "fierycod/holy-lambda   \"" new-version "\""))))
    (spit "project.clj" (s/replace (slurp "project.clj") PROJECT_VERSION (str "fierycod/holy-lambda   \"" new-version "\"")))
    (spit "VERSION" new-version)))

(deploy {:type (first *command-line-args*)})
