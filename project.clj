(defproject fierycod/holy-lambda "0.0.1"
  :description "Micro framework which turns your code into AWS Lambda functions"
  :url "https://github.com/FieryCod/holy-lambda"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.macro "0.1.2"]
                 [com.amazonaws/aws-lambda-java-core "1.2.0"]]
  :aot :all)
