(ns fierycod.holy-lambda.impl.util
  (:require
   [clojure.data.json :as json])
  (:import
   [java.net URL HttpURLConnection]
   [java.io InputStream OutputStream InputStreamReader]
   )

  )

(def ^:private success-codes #{200 202})

(defn- retrieve-body
  [^HttpURLConnection http-conn status]
  (if-not (success-codes status)
    (.getErrorStream http-conn)
    (.getInputStream http-conn)))

(defn in->edn-event
  [^InputStream event]
  (json/read (InputStreamReader. event "UTF-8") :key-fn keyword))

(defn success-code?
  [code]
  (success-codes code))

(defn http
  [method url-s & [payload]]
  (let [push? (= method "POST")
        ^String payload-s (when push? (if (string? payload) payload
                                          (json/write-str (assoc payload
                                                                 :body (json/write-str (:body payload))))))
        ^HttpURLConnection http-conn (-> url-s (URL.) (.openConnection))
        _ (doto http-conn
            (.setDoOutput push?)
            (.setRequestProperty "Content-Type" "application/json")
            (.setRequestMethod method))
        _ (when push?
            (doto (.getOutputStream http-conn)
              (.write (.getBytes payload-s "UTF-8"))
              (.close)))
        headers (into {} (.getHeaderFields http-conn))
        status (.getResponseCode http-conn)]
    {:headers headers
     :status status
     :body (in->edn-event (retrieve-body http-conn status))}))

(defn call
  ([afn-sym]
   (partial call afn-sym))
  ([afn-sym & args]
   (let [{:keys [arity ns name]} (meta afn-sym)]
     (assert (= arity (count args))
             (str "Function defined with two arguments should call lambda with only two arguments. "
                  "Otherwise use Lambada style and call with three arguments.\n\n"
                  "Failed when calling: '" ns "." name "\n"))
     (apply afn-sym args))))
