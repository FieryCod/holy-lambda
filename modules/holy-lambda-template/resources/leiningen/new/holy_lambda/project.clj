(defproject {{name}} "0.0.1-SNAPSHOT"
  :description "Huh. In the beginning was darkness :)"
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [io.github.FieryCod/holy-lambda   "0.1.28"]
                 ;; for babashka use to make smaller scripts
                 ;; [io.github.FieryCod/holy-lambda-babashka   "SAME_VERSION_AS_REGULAR_HOLY_LAMBDA_DEPENDENCY"]
                 ]
  :main ^:skip-aot {{sanitized}}.core
  :profiles {:uberjar {:aot :all}}
  :uberjar-name "output.jar")
