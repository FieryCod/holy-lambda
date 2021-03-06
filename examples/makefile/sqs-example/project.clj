(defproject sqs-example "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure         "1.10.3"]
                 [io.github.FieryCod/holy-lambda   "0.2.0-SNAPSHOT"]
                 [com.cognitect.aws/api       "0.8.505"]
                 [com.cognitect.aws/endpoints "1.1.11.1001"]
                 [com.cognitect.aws/sqs       "811.2.889.0"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot sqs-example.core
  :profiles {:uberjar {:aot      :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"
                                  "-Dclojure.spec.skip-macros=true"]}}
  :uberjar-name "output.jar")
