(defproject fierycod/holy-lambda "0.0.7-SNAPSHOT"
  :description "Micro framework which turns your code into AWS Lambda functions"

  :url "https://github.com/FieryCod/holy-lambda"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :source-paths ["src/clj"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.macro "0.1.5"]
                 [com.amazonaws/aws-lambda-java-core "1.2.0"]]

  :eftest {:thread-count 4}

  :plugins [[lein-cloverage "1.1.1"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]

  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda"}

  :profiles {:eftest {:global-vars {*warn-on-reflection* false}
                      :plugins [[lein-eftest "0.5.8"]]}
             :uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
