(ns leiningen.new.holy-lambda
  (:require
   [clojure.java.shell :as sh]
   [leiningen.new.templates :refer [renderer name-to-path ->files]]
   [leiningen.core.main :as main]))

(def render (renderer "holy-lambda"))

(defn holy-lambda
  [name]
  (let [data {:name name
              :sanitized (name-to-path name)}
        render* #(render % data)]
    (main/info "Generating fresh 'lein new' holy-lambda project.")
    (->files data
             ["src/{{sanitized}}/core.cljc" (render* "core.cljc")]
             [".clj-kondo/config.edn" (render* "clj-kondo/config.edn")]
             [".clj-kondo/clj_kondo/holy_lambda.clj" (render* "clj-kondo/clj_kondo/holy_lambda.clj")]
             ["resources/native-agents-payloads/1.edn" (render* "1.edn")]
             ["README.md" (render* "README.md")]
             ["bb.edn" (render* "bb.edn")]
             ["deps.edn" (render* "deps.edn")]
             ["envs.json" (render* "envs.json")]
             ["template.yml" (render* "template.yml")]
             [".editorconfig" (render* "editorconfig")]
             [".gitignore" (render* "gitignore")])))
