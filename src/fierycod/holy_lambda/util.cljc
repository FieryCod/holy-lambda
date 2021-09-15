(ns ^:no-doc ^:private fierycod.holy-lambda.util
  (:require
   [fierycod.holy-lambda.retriever :as retriever]
   #?(:bb [cheshire.core :as json]
      :clj [jsonista.core :as json])
   [fierycod.holy-lambda.util :as u])
  #?(:clj
     (:import
      [java.util List HashMap]
      [clojure.lang PersistentHashMap]
      [java.net URL HttpURLConnection]
      [java.io InputStream InputStreamReader])))

(defn- compress-strings
  [args]
  (into
   []
   (comp
    (partition-by string?)
    (mapcat (fn [p] (if (string? (first p)) [(apply str p)] p))))
   args))

(defmacro ->str
  [& args]
  (let [args (compress-strings args)]
    (if (== 1 (count args))
      `(.toString ~(first args))
      (let [sb (gensym "sb__")
            appends (for [arg args
                          :when arg]
                      (if (string? arg)
                        `(.append ~sb ~arg)
                        `(.append ~sb (.toString ~arg))))]
        `(let [~sb (StringBuilder.)]
           ~@appends
           (.toString ~sb))))))

(defmacro ->ex
  [& args]
  `(Exception. (->str "[holy-lambda]: " ~@args)))

(defn println-err!
  [msg]
  (binding [*out* *err*]
    (println msg)))

(defn x->json-string
  [x]
  (if (string? x)
    x
    #?(:bb (json/generate-string x)
       :clj (json/write-value-as-string x)
       :default (throw (->ex "Not implemented")))))

(defn adopt-map
  [^HashMap m]
  #?(:clj (PersistentHashMap/create m)
     :bb (PersistentHashMap/create m)
     :cljs (into {} m)))

(defn x->json-bytes
  [x]
  #?(:bb (.getBytes (json/generate-string x))
     :clj (json/write-value-as-bytes x)
     :default (throw (->ex "Not implemented"))))

(defn json-stream->x
  [^InputStream s]
  #?(:bb
     (json/parse-string (slurp (InputStreamReader. s "UTF-8")) true)
     :clj
     (json/read-value
      (InputStreamReader. s "UTF-8")
      json/keyword-keys-object-mapper)
     :default
     (throw (->ex "Not implemented"))))

(defn json-string->x
  [^String s]
  #?(:bb
     (json/parse-string s true)
     :clj
     (json/read-value s json/keyword-keys-object-mapper)
     :default
     (throw (->ex "Not implemented"))))

(defn- normalize-headers
  [headers]
  (into {} (keep (fn [[k v]] (when k [(.toLowerCase (name k)) v]))) headers))

(defn response-event->normalized-event
  [event]
  (cond-> event
    (seq (:headers event))
    (update :headers normalize-headers)

    (seq (:multiValueHeaders event))
    (update :multiValueHeaders normalize-headers)))

(defn- content-type
  [x]
  (get (get x :headers) "content-type"))

(defn- json-content-type?
  [ctype]
  (= ctype "application/json"))

(defn in->edn-event
  [^InputStream event-stream]
  (let [event (-> event-stream json-stream->x)
        ctype (content-type event)
        body (event :body)]
    (cond-> event
      (and (string? body)
           (json-content-type? ctype))
      (assoc :body (json-string->x body)))))

(defn response->bytes
  [?response]
  (let [response (retriever/<-wait-for-response ?response)
        bytes-response? #?(:clj (bytes? response)
                           :default false)
        ctype (when-not bytes-response?
                (content-type response))]

    (cond
      bytes-response?
      response

      ;; Optimize the common case
      (json-content-type? ctype)
      (x->json-bytes (update response :body x->json-string))

      ;; Ack event
      (nil? response)
      (x->json-bytes {:body nil
                      :statusCode 200})

      :else 
      (x->json-bytes response))))

#?(:clj
   (defn http
     [method url-s & [response]]
     (let [push? (.equals "POST" method)
           response-bytes (when push? (response->bytes (response-event->normalized-event response)))
           ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))]
       (doto ^HttpURLConnection http-conn
         (.setDoOutput push?)
         (.setRequestProperty "content-type" "application/json")
         (.setRequestMethod method))
       (when push?
         (let [output-stream (.getOutputStream http-conn)]
           (if (bytes? response-bytes)
             (.write output-stream ^"[B" response-bytes)
             (do (println "[holy-lambda] Response has not beed parsed to bytes array:" response-bytes)
                 (throw (->ex "Failed to parse response to byte array. Response type:" (str (type response-bytes))))))
           (.flush output-stream)
           (.close output-stream)))
       ;; It's not necessary to normalize the response headers for runtimes since
       ;; we only rely on Lambda-Runtime-Deadline-Ms and Lambda-Runtime-Aws-Request-Id headers
       (let [headers (adopt-map (.getHeaderFields http-conn))
             status (.getResponseCode http-conn)
             success? (case status (200 201 202) true false)]
         {:headers headers
          :success? success?
          :status status
          :body (response-event->normalized-event
                 (in->edn-event
                  (if success?
                    (.getInputStream http-conn)
                    (.getErrorStream http-conn))))}))))

(defn getf-header
  [headers prop]
  #?(:clj (when-let [^List l (get headers prop)] (.get l 0))
     :bb (some-> (get headers prop) seq first)
     :cljs (some-> (get headers prop) seq first)))

(defn exit!
  []
  (System/exit 1))
