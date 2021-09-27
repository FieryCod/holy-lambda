(require '[clj-yaml.core :as yaml]
         '[babashka.curl :as curl]
         '[clojure.string :as s]
         '[babashka.process :as proc]
         '[babashka.fs :as fs])

(def conf-file (read-string (slurp (str "conf"
                                        (or (first *command-line-args*) "128")
                                        ".edn"))))

(def COLD? (if (System/getenv "COLD") "1" "0"))

(def results (atom {}))
(def memory-size (:memory-size conf-file))
(def BASE_URL (:url conf-file))
(def file (some-> (some->> memory-size
                           (str "packaged-"))
                  (str ".yml")))

(def IGNORED_FNS (set (mapv (fn [x] (str x "-" memory-size)) (:ignored-fns conf-file))))

;; (when-not (fs/exists? file)
;;   (println "File" file "doesn't exists! Unable to run benchmarks!")
;;   (System/exit 1))
(def yml (yaml/parse-string (slurp file)))
(def S3_PATTERN #"s3://([a-zA-Z0-9-]+)/([a-zA-Z0-9]+)")
(def resources (:Resources yml))
(def functions<->props
  (into {} (mapv (fn [[k _props]]
                   (let [props (:Properties _props)
                         path (:Path (:Properties (:HelloEvent (:Events props))))
                         codeuri (:CodeUri props)
                         [bn bk] (rest (re-matches S3_PATTERN (or codeuri "")))]
                     [(str (name k) "-" memory-size)
                      {:bucket-name bn
                       :fn (str (name k) "-" memory-size)
                       :path path
                       :image-uri (:ImageUri props)
                       :bucket-key bk}]))
                 (filter (fn [[k _v]]
                           (not (s/includes? (s/lower-case (str k)) "local")))
                         resources))))

(defn api-call
  [{:keys [path fn bucket-key bucket-name]} cold]
  (try
    (let [{:keys [status body]} (curl/get (str BASE_URL path) {:raw-args ["-w" ",%{time_total}"]})
          time (Double/parseDouble (second (s/split body #",")))]
      (swap! results update fn (fnil conj []) [status time cold]))
    (catch Exception _
      (swap! results update fn (fnil conj []) [500 -1 cold]))))

(def WRITE_PATH (str "results/" "memory-" memory-size "-cold--" (if (= COLD? "1") "yes" "no") ".csv"))

(defn write-csv!
  []
  (let [csv-content (StringBuilder. "function_name,memory_size,status,is_cold,time_s\n")
        results @results]
    (doseq [group (seq results)]
      (doseq [[status time cold] (second group)]
        (.append csv-content ^String (str (first group) ","))
        (.append csv-content ^String (str memory-size ","))
        (.append csv-content ^String (str status ","))
        (.append csv-content ^String (str cold ","))
        (.append csv-content ^String (str time "\n"))))
    (spit WRITE_PATH (.toString csv-content))))

(defn force-update-fn!
  [{:keys [bucket-name bucket-key image-uri fn]}]
  (if-not image-uri
    (do
      (Thread/sleep 1000)
      (:exit (proc/sh ["aws"
                       "lambda"
                       "update-function-code"
                       "--function-name" fn
                       "--s3-bucket" bucket-name
                       "--s3-key" bucket-key])))
    (do
      (Thread/sleep 45000)
      (:exit (proc/sh ["aws"
                       "lambda"
                       "update-function-code"
                       "--function-name" fn
                       "--image-uri" image-uri
                       "--publish"])))))

(doseq [i (range 1000)]
  (println "Iteration =" (inc i))
  (doseq [f (mapv (fn [[s fn-prop]]
                    (println "Iteration =" (inc i) ", fn-prop=" fn-prop)
                    (future
                      (when-not (contains? IGNORED_FNS s)
                        (if (= COLD? "1")
                          (api-call fn-prop (if (= (force-update-fn! fn-prop) "0") "1" "0"))
                          (api-call fn-prop "0")))))
                  functions<->props)]
    @f))

(write-csv!)
