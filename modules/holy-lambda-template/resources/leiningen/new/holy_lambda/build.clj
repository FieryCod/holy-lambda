(ns build
  (:require
   [clojure.tools.build.api :as b]))

(def class-dir ".holy-lambda/target/classes")
(def basis (b/create-basis {:project "deps.edn"}))

(defn clean [_]
  (b/delete {:path ".holy-lambda/target"})
  (b/delete {:path ".holy-lambda/build"}))

(defn uber [_]
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir class-dir})
  (b/uber {:class-dir class-dir
           :main '{{main-ns}}.core
           :basis basis
           :uber-file ".holy-lambda/build/output.jar"}))
