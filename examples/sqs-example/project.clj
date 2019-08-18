(defproject sqs-example "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [fierycod/holy-lambda "0.0.6"]
                 [com.amazonaws/aws-java-sdk-bom "1.11.613" :extension "pom" :scope "import"]
                 [com.amazonaws/aws-java-sdk-sqs "1.11.613"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot sqs-example.core
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}}
  :uberjar-name "output.jar")
