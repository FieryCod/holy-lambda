(require '[selmer.parser :as selm])
(require '[babashka.process :as p])

(def BABASHKA_VERSION "0.6.8")

(defn- exit-non-zero
  [proc]
  (when-let [exit-code (some-> proc deref :exit)]
    (when (not (zero? exit-code))
      (System/exit exit-code))))

(defn- shell
  [cmd]
  (exit-non-zero (p/process (p/tokenize cmd) {:inherit true})))

(defn- shell-no-exit
  [inherit? cmd & args]
  (p/process (into (p/tokenize cmd) (remove nil? args)) {:inherit inherit?}))

(defn should-build?
  []
  (= 1 (:exit @(shell-no-exit false "git" "diff" "--exit-code" "HEAD~1" "HEAD" "--" "."))))

(def CE_IMAGES
  [{:version ["21.2.0" "21.1.0" "21.3.0"]
    :java    ["11"]
    :arch    ["amd64", "aarch64"]}
   {:version ["21.3.0"]
    :java    ["17"]
    :arch    ["amd64", "aarch64"]}
   {:version ["21.2.0" "21.1.0"]
    :java    ["8"]
    :arch    ["amd64"]}])

(def DEV_IMAGES
  [{:version         "22.0.0-dev-20211201_2302",
    :partial-version "22.0.0-dev",
    :java            ["11" "17"],
    :arch            ["amd64", "aarch64"]},
   {:version         "22.0.0-dev-20211201_0026",
    :partial-version "22.0.0-dev",
    :java            ["11" "17"],
    :arch            ["amd64", "aarch64"]}
   {:version         "22.0.0-dev-20211129_1955",
    :partial-version "22.0.0-dev",
    :java            ["11" "17"],
    :arch            ["amd64", "aarch64"]}])

(defn dev-image-url
  [{:keys [version java arch]}]
  (str "https://github.com/graalvm/graalvm-ce-dev-builds/releases/download/" version "/graalvm-ce-java" java "-linux-" arch "-dev.tar.gz"))

(defn ce-image-url
  [{:keys [version java arch]}]
  (str "https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-" version "/graalvm-ce-java" java "-linux-" arch "-" version ".tar.gz"))

(defn build-pub-ce!
  [{:keys [java version arch] :as spec}]
  (let [dockerfile          "Dockerfile-ce-dev"
        dockerfile-template (str dockerfile ".template")
        dockerfile-content  (selm/render (slurp dockerfile-template)
                                         (assoc spec
                                                :bb-version BABASHKA_VERSION
                                                :java-home (str "/opt/graalvm-ce-java" java "-" version)
                                                :graalvm-url (ce-image-url spec)
                                                :image-prefix (if-not (= arch "aarch64") "" "arm64v8/")
                                                :additional-components (if (= arch "aarch64")
                                                                         ""
                                                                         " python ruby R")))
        image-uri           (str "ghcr.io/fierycod/holy-lambda-builder:" arch "-java" java "-" version)]
    (spit dockerfile dockerfile-content)
    (println "> Building:" image-uri)
    (shell (str "docker build . -f " dockerfile " -t " image-uri (when (= arch "aarch64") " --platform linux/aarch64 --pull")))
    (println "> Publishing:" image-uri)
    (shell (str "docker push " image-uri))))

(defn build-pub-dev!
  [{:keys [java version arch partial-version] :as spec}]
  (let [dockerfile          "Dockerfile-ce-dev"
        graalvm-url         (dev-image-url spec)
        dockerfile-template (str dockerfile ".template")
        dockerfile-content  (selm/render (slurp dockerfile-template)
                                         (assoc spec
                                                :java-home (str "/opt/graalvm-ce-java" java "-" partial-version)
                                                :bb-version BABASHKA_VERSION
                                                :image-prefix (if-not (= arch "aarch64") "" "arm64v8/")
                                                :graalvm-url graalvm-url
                                                :additional-components (if (= arch "aarch64")
                                                                         ""
                                                                         " python ruby R")))
        image-uri           (str "ghcr.io/fierycod/holy-lambda-builder:" arch "-java" java "-" version)]
    (spit dockerfile dockerfile-content)
    (println "> Building:" image-uri)
    (shell (str "docker build . -f " dockerfile " -t " image-uri (when (= arch "aarch64") " --platform linux/aarch64 --pull")))
    (println "> Publishing:" image-uri)
    (shell (str "docker push " image-uri))))

(def requested-arch (first *command-line-args*))
(def CE (= (second *command-line-args*) "CE"))

(when (should-build?)
  (when CE
    (doseq [{:keys [version java arch]} CE_IMAGES]
      (doseq [version version]
        (doseq [java java]
          (doseq [arch arch]
            (when (= requested-arch arch)
              (build-pub-ce! {:version version
                              :java    java
                              :arch    arch})))))))

  (when-not CE
    (doseq [{:keys [version java arch partial-version]} DEV_IMAGES]
      (doseq [java java]
        (doseq [arch arch]
          (when (= requested-arch arch)
            (build-pub-dev! {:version         version
                             :partial-version partial-version
                             :java            java
                             :arch            arch})))))))
