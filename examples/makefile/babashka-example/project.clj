(defproject babashka-example "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [io.github.FieryCod/holy-lambda   "0.1.19" ]]
  :global-vars {*warn-on-reflection* true}
  :main ^:skip-aot babashka-example.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "output.jar")
