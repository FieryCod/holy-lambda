(defproject hello-lambda "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [fierycod/holy-lambda "0.0.8"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot hello-lambda.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "output.jar")
