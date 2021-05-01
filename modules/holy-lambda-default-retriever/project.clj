(defproject io.github.FieryCod/holy-lambda-default-retriever "0.0.5"
  :description "Default retriever for payloads"

  :url "https://github.com/FieryCod/holy-lambda/tree/master/modules/holy-lambda-default-retriever"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :source-paths ["src"]

  :global-vars {*warn-on-reflection* true}

  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]]

  :eftest {:thread-count 4}

  :plugins [[lein-cloverage "1.1.1"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]

  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda"
        :dir "../.."}

  :profiles {:eftest {:global-vars {*warn-on-reflection* false}
                      :plugins [[lein-eftest "0.5.9"]]}
             :uberjar {:jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}})
