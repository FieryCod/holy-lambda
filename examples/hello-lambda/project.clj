(defproject hello-lambda "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.3.610"]
                 [fierycod/holy-lambda   "0.1.7"]
                 [fierycod/holy-lambda-async-retriever "0.0.1"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot hello-lambda.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "output.jar")
