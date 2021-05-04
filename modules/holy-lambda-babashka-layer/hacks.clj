(require '[babashka.deps])

(alter-var-root #'babashka.deps/add-deps
                (fn [f]
                  (fn [m]
                    (f (merge m {:mvn/local-repo "/opt/.m2"})))))
