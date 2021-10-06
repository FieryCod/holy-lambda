(require '[selmer.parser :as selm])
(require '[babashka.process :as p])

(defn- exit-non-zero
  [proc]
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell
  [cmd]
  (exit-non-zero (p/process (p/tokenize cmd) {:inherit true})))

(def VERSIONS
  {"amd64" {"ce" ["java8-21.2.0"
                  "java11-21.2.0"]}})

(defn build-pub-ce!
  [arch variant version]
  (let [dockerfile          (str "Dockerfile" "." arch "." variant)
        dockerfile-template (str dockerfile ".template")
        dockerfile-content  (selm/render (slurp dockerfile-template) {:version version})
        image-uri (str "ghcr.io/fierycod/holy-lambda-builder:" arch "-" variant "-" version)]
    (spit dockerfile dockerfile-content)
    (println "> Building:" image-uri)
    (shell (str "docker build . -f " dockerfile " -t " image-uri))
    (println "> Publishing:" image-uri)
    (shell (str "docker push " image-uri))))

(println "Building & Publishing AMD64 CE Images")
(doseq [version (get-in VERSIONS ["amd64" "ce"])]
  (build-pub-ce! "amd64" "ce" version))
