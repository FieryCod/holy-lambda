(require '[babashka.pods :as pods])
(require '[clojure.java.io :as io])
(require '[clojure.edn :as edn])

(def OPTIONS (edn/read-string (slurp (io/file "/project/bb.edn"))))

(doseq [[s v] (:pods (:runtime (:holy-lambda/options OPTIONS)))]
  (pods/load-pod s v))
