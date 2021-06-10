(ns leiningen.new.holy-lambda
  (:require
   [leiningen.new.templates :refer [renderer
                                    project-name
                                    name-to-path
                                    ->files
                                    multi-segment
                                    sanitize-ns]]
   [clojure.string :as string]
   [leiningen.core.main :as main])
  (:import
   [java.util UUID]))

(def render (renderer "holy-lambda"))

(defn holy-lambda
  [name]
  (let [uuid (subs (string/replace (.toString (UUID/randomUUID)) #"-" "") 0 6)
        pname (project-name name)
        main-ns (string/replace (multi-segment (sanitize-ns name)) #".core" "")
        data {:name (project-name name)
              :nested-dirs (name-to-path main-ns)
              :main-ns main-ns
              :sanitized (name-to-path name)
              :bucket-name (string/replace (str pname "-" uuid) #"\." "-")
              :stack-name (string/replace (str pname "-" uuid "-stack") #"\." "-")
              :bucket-prefix "holy-lambda"}
        render* #(render % data)]
    (main/info "Generating new project based on holy-lambda. Make sure that you have babashka tool installed, `docker` running and AWS account properly configured via aws configure.

First steps in new project:
- 1. Choose a runtime in holy-lambda runtime in bb.edn file. :runtime:name
- 2. Change the name of the bucket and stack
- 3. Run bb stack:sync to sync the project with dockerized version of holy-lambda
- 4. Run bb tasks to get full list of tasks")

    (->files data
             ["src/{{nested-dirs}}/core.cljc" (render* "core.cljc")]
             [".clj-kondo/config.edn" (render* "clj-kondo/config.edn")]
             [".clj-kondo/clj_kondo/holy_lambda.clj" (render* "clj-kondo/clj_kondo/holy_lambda.clj")]
             ["resources/native-agents-payloads/1.edn" (render* "1.edn")]
             ["README.md" (render* "README.md")]
             ["bb.edn" (render* "bb.edn")]
             ["deps.edn" (render* "deps.edn")]
             ["envs.json" (render* "envs.json")]
             ["template.yml" (render* "template.yml")]
             [".editorconfig" (render* "editorconfig")]
             [".gitignore" (render* "gitignore")])))
