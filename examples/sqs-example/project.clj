(defproject sqs-example "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [fierycod/holy-lambda "0.0.2"]
                 [com.amazonaws/aws-java-sdk-bom "1.11.568" :extension "pom" :scope "import"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.568"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot sqs-example.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "sqs-example.jar")
