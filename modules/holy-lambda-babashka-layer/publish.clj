(require '[selmer.parser :as selm])
(require '[babashka.process :as p])
(require '[clojure.string :as s])

(def ARM64 "arm64")
(def AMD64 "amd64")

(def ARCHS #{ARM64 AMD64})
(def FILES #{"README.md.template" "Dockerfile.bb.template" "template.yml.template"})

(def BABASHKA_VERSION "0.8.0")
(def SEMANTIC_VERSION "0.6.7")

(defn arch->subcord
  [arch]
  (get {"arm64" "aarch64" "amd64" "amd64"} arch))

(defn template->filename
  [template arch]
  (let [[file ext] (s/split (subs template 0 (- (count template) 9)) #"\.")]
    (str file "-" arch "." ext)))

(defn render
  [template arch]
  (spit (template->filename template arch)
        (selm/render (slurp template)
                     {:arch arch
                      :semantic-version SEMANTIC_VERSION
                      :babashka-version BABASHKA_VERSION
                      :aarch (arch->subcord arch)
                      :arch_is (if (= arch "arm64") "arm64" "x86_64")})))

(defn- shell
  [cmd]
  @(p/process (p/tokenize cmd) {:inherit true}))

(def task (first *command-line-args*))

(when (or (= task "render")
          (= task "all"))
  (doseq [arch ARCHS]
    (doseq [file FILES]
      (render file arch))))

(when (or (= task "build")
          (= task "all"))
  (doseq [arch ARCHS]
    (shell (str "docker build . --target BUILDER -t holy-lambda-babashka-layer-" arch  " -f Dockerfile-" arch ".bb"))
    (shell "bash -c \"docker rm build || true\"")
    (shell (str "docker create --name build holy-lambda-babashka-layer-" arch))
    (shell (str "docker cp build:/opt/holy-lambda-babashka-runtime-" arch ".zip holy-lambda-babashka-runtime-" arch ".zip"))
    (shell "bash -c \"docker rm build || true\"")
    (shell (str "docker image rm -f holy-lambda-babashka-layer-" arch))))

(when (or (= task "publish")
          (= task "all"))
  (doseq [arch ARCHS]
    (let [arch      arch
          version   SEMANTIC_VERSION
          s3        "holy-lambda-babashka-layer"
          s3-prefix "holy-lambda"
          region    "eu-central-1"
          babashka-version BABASHKA_VERSION
          semantic-version SEMANTIC_VERSION]
      (shell (selm/<< "sam package --template-file template-{{arch}}.yml --output-template-file packaged-{{arch}}.yml --s3-bucket {{s3}} --s3-prefix {{s3-prefix}}"))
      (shell (selm/<< "sam publish --template-file packaged-{{arch}}.yml --semantic-version {{version}}")))))
