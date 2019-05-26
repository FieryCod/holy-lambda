(defproject sqs-example "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1-RC1"]
                 [fierycod/holy-lambda "0.0.2"]
                 [commons-logging/commons-logging "1.2"]
                 [log4j/log4j "1.2.17"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.560"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot sqs-example.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "sqs-example.jar")
