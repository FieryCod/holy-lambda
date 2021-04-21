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
             ["resources/native-deps/.gitkeep" (render* "gitkeep")]
             ["resources/native-agents-payloads/1.edn" (render* "1.edn")]
             ["Makefile" (render* "Makefile")]
             ["README.md" (render* "README.md")]
             ["project.clj" (render* "project.clj")]
             ["template.yml" (render* "template.yml")]
             ["template-native.yml" (render* "template-native.yml")]
             ["resources/bootstrap-native" (render* "bootstrap-native")]
             ["resources/bootstrap-native-babashka" (render* "bootstrap-native-babashka")]
             [".editorconfig" (render* "editorconfig")]
             [".gitignore" (render* "gitignore")])
    (main/info "Copying bb from /usr/bin/bb")
    (sh "cp /usr/bin/bb bb" :dir (str (name-to-path name) "/resources/native-deps/"))))
