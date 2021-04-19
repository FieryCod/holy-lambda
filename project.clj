(defproject io.github.FieryCod/holy-lambda   "0.1.13"
  :description "Micro framework which turns your code into AWS Lambda functions"

  :url "https://github.com/FieryCod/holy-lambda"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :source-paths ["src"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.0" :scope "provided"]
                 [metosin/jsonista "0.3.1"]
                 [com.amazonaws/aws-lambda-java-core "1.2.1"]
                 [io.github.FieryCod/holy-lambda-default-retriever "0.0.2"]]

  :eftest {:thread-count 4}

  :plugins [[lein-cloverage "1.1.1"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]
  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda"}

  :profiles {:eftest {:global-vars {*warn-on-reflection* false}
                      :plugins [[lein-eftest "0.5.9"]]}
             :uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
