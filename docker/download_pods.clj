(require '[babashka.pods :as pods])
(require '[clojure.java.io :as io])
(require '[clojure.edn :as edn])

(def OPTIONS (edn/read-string (slurp (io/file "/project/bb.edn"))))

(doseq [[s v] (:pods (or (:runtime (:holy-lambda/options OPTIONS))
                         (:backend (:holy-lambda/options OPTIONS))))]
  (println "[holy-lambda] Downloading pod =" s "v =" v)
  (pods/load-pod s v))
