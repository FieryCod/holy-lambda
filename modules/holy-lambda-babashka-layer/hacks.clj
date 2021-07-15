(require '[babashka.deps])

(alter-var-root #'babashka.deps/add-deps
                (fn [f]
                  (fn [m]
                    (println "[holy-lambda] Dependencies should not be added via add-deps. Move your dependencies to a layer!")
                    (System/exit 1)
                    ;; (f (merge m {:mvn/local-repo "/opt/.m2"}))
                    )))
