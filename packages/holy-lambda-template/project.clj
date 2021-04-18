(defproject holy-lambda/lein-template "0.0.11"
  :description "Template for holy lambda micro framework"

  :url "https://github.com/FieryCod/holy-lambda-template"

  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :creds :gpg}
                         "snapshots" {:url "https://clojars.org/repo"
                                      :creds :gpg}]]

  :scm {:name "git"
        :url "https://github.com/FieryCod/holy-lambda-template"}

  :eval-in-leiningen true)
