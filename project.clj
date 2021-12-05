(defproject io.github.FieryCod/holy-lambda   "0.6.3"
  :description "Micro framework which turns your code into AWS Lambda functions"

  :url "https://github.com/FieryCod/holy-lambda"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :source-paths ["src"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure                              "1.10.3" :scope "provided"]
                 [metosin/jsonista                                 "0.3.4"]
                 [io.github.FieryCod/holy-lambda-default-retriever "0.5.0"]]

  :resources ["resources"]

  :eftest {:thread-count 4}

  :plugins [[lein-cloverage "1.1.1"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]
  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda"}

  :cloverage {:runner :eftest
              :runner-opts {:test-warn-time 500
                            :fail-fast? true
                            :multithread? :namespaces}}

  :profiles {:eftest {:resource-paths ["resources-test"]
                      :global-vars {*warn-on-reflection* false}
                      :dependencies [[eftest/eftest "0.5.9"]]
                      :plugins [[lein-eftest "0.5.9"]]}
             :uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
