(defproject io.github.FieryCod/holy-lambda-babashka-shim "0.1.42"
  :description "Micro framework which turns your code into AWS Lambda functions suites for babashka based Lambdas"

  :url "https://github.com/FieryCod/holy-lambda/tree/master/modules/holy-lambda-babashka"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :source-paths ["../../src" "src/clj"]
  :java-source-paths ["src/java"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [io.github.FieryCod/holy-lambda-default-retriever "0.0.5"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]
  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda"}
)
