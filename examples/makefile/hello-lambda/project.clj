(defproject hello-lambda "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/core.async "1.3.610"]
                 [io.github.FieryCod/holy-lambda   "0.2.0-SNAPSHOT" :exclusions [io.github.FieryCod/holy-lambda-default-retriever]]
                 [io.github.FieryCod/holy-lambda-async-retriever "0.0.6"]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot hello-lambda.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "output.jar")
