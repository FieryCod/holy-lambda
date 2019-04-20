(defproject fierycod/holy-lambda "0.0.1"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "MIT"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.macro "0.1.2"]
                 [com.amazonaws/aws-lambda-java-log4j2 "1.0.0"]
                 [org.apache.logging.log4j/log4j-api "2.8.2"]
                 [org.apache.logging.log4j/log4j-core "2.8.2"]
                 [com.amazonaws/aws-lambda-java-core "1.2.0"]]
  :aot :all)
