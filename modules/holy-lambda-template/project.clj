(defproject holy-lambda/lein-template   "0.1.30-SNAPSHOT"
  :description "Template for holy lambda micro framework"

  :url "https://github.com/FieryCod/holy-lambda/tree/master/packages/holy-lambda-template"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]

  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda/tree/master/packages/holy-lambda-default-retriever"}

  :eval-in-leiningen true)
