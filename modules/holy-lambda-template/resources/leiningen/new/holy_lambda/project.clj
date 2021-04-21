(defproject {{name}} "0.0.1-SNAPSHOT"
  :description "Huh. In the beginning was darkness :)"
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "1.3.610"]
                 [io.github.FieryCod/holy-lambda   "0.1.23"]]
  :main ^:skip-aot {{sanitized}}.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "output.jar")
