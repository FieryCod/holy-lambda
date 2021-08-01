(require '[clj-yaml.core :as yaml]
         '[babashka.curl :as curl]
         '[clojure.string :as s]
         '[babashka.fs :as fs])

(def conf-file (read-string (slurp (str "conf"
                                        (or (first *command-line-args*) "128")
                                        ".edn"))))

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
                         [bn bk] (rest (re-matches S3_PATTERN codeuri))]
                     [(str (name k) "-" memory-size)
                      {:bucket-name bn
                       :fn (str (name k) "-" memory-size)
                       :path path
                       :bucket-key bk}]))
                 resources)))

(defn api-call
  [{:keys [path fn bucket-key bucket-name]}]
  (try
    (let [{:keys [status body]} (curl/get (str BASE_URL path) {:raw-args ["-w" ",%{time_total}"]})
          time (Double/parseDouble (second (s/split body #",")))]
      (swap! results update fn (fnil conj []) [status time]))
    (catch Exception _
      (swap! results update fn (fnil conj []) [500 -1]))))

(def WRITE_PATH (str "results/" "memory-" memory-size ".csv"))


(defn write-csv!
  []
  (let [csv-content (StringBuilder. "function_name,memory_size,status,time_ms\n")
        results @results]
    (doseq [group (seq results)]
      (doseq [[status time] (second group)]
        (.append csv-content ^String (str (first group) ","))
        (.append csv-content ^String (str memory-size ","))
        (.append csv-content ^String (str status ","))
        (.append csv-content ^String (str time "\n"))))
    (spit WRITE_PATH (.toString csv-content))))

(write-csv!)

(doseq [i (range 2)]
  (doseq [[s fn-prop] functions<->props]
    (when-not (contains? IGNORED_FNS s)
      (api-call fn-prop))
    )
  )



(println @results)
