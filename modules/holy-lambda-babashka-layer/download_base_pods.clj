(require '[babashka.pods :as pods])
(require '[clojure.java.io :as io])
(require '[clojure.edn :as edn])
(require '[clojure.java.shell :refer [sh]])

(def DEPS (edn/read-string (slurp (io/file "./deps.edn"))))

(doseq [[s v] (:pods DEPS)]
  (pods/load-pod s v))

