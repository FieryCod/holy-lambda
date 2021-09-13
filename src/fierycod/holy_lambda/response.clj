(ns fierycod.holy-lambda.response
  "Response helpers adapted from ring-core/util/response.clj"
  (:require
   [clojure.string :as s]))

(def redirect-status-codes
  "Map a keyword to a redirect status code."
  {:moved-permanently  301
   :found              302
   :see-other          303
   :temporary-redirect 307
   :permanent-redirect 308})

(defn redirect
  "Returns a response for an HTTP 302 redirect. Status may be
   a key in redirect-status-codes or a numeric code. Defaults to 302"
  ([url] (redirect url :found))
  ([url status]
   {:statusCode (redirect-status-codes status status)
    :headers    {"location" url}
    :body       nil}))

(defn png-image
  [base64-str]
  {:isBase64Encoded true
   :body            base64-str
   :statusCode      200
   :headers         {"content-type" "image/png"}})

(defn created
  "Returns a response for a HTTP 201 created response."
  {:added "1.2"}
  ([url] (created url nil))
  ([url body]
   {:statusCode 201
    :headers    {"location" url}
    :body       body}))

(defn bad-request
  "Returns a 400 'bad request' response."
  [body]
  {:statusCode 400
   :headers    {}
   :body       body})

(defn not-found
  "Returns a 404 'not found' response."
  [body]
  {:statusCode 404
   :headers    {}
   :body       body})

(defn response
  "Returns a skeletal response with the given body, status of 200, and no
  headers."
  [body]
  {:statusCode 200
   :headers    {}
   :body       body})

(defn json
  "Returns a skeletal response with the given body, status of 200, and `content-type` set to `application/json`."
  [body]
  {:statusCode 200
   :headers    {"content-type" "application/json"}
   :body       body})

(defn text
  "Returns a skeletal response with the given msg, status of 200, and `content-type` set to `text/plain`."
  [?msg]
  {:statusCode 200
   :headers    {"content-type" "text/plain; charset=utf-8"}
   :body       ?msg})

(defn html
  "Returns a skeletal response with the given body, status of 200, and `content-type` set to `text/html`."
  [?body]
  {:statusCode 200
   :headers    {"content-type" "text/html; charset=utf-8"}
   :body       ?body})

(defn status
  "Returns an updated response with the given status."
  ([?status]
   {:statusCode ?status
    :headers    {}
    :body       {}})
  ([resp ?status]
   (assoc resp :statusCode ?status)))

(defn header
  "Returns an updated response with the specified header added."
  [resp ?name value]
  (assoc-in resp [:headers ?name] (str value)))

(defn content-type
  "Returns an updated response with the a content-type header corresponding
  to the given content-type."
  [resp ?content-type]
  (header resp "content-type" ?content-type))

(defn find-header
  "Looks up a header in a response (or request) case insensitively,
  returning the header map entry, or nil if not present."
  [resp ^String header-name]
  (->> (:headers resp)
       (filter #(.equalsIgnoreCase header-name (key %)))
       (first)))

(defn get-header
  "Looks up a header in a response (or request) case insensitively,
  returning the value of the header, or nil if not present."
  [resp ^String header-name]
  (some-> resp (find-header header-name) val))

(defn update-header
  "Looks up a header in a response (or request) case insensitively,
  then updates the header with the supplied function and arguments in the
  manner of update-in."
  [resp header-name f & args]
  (let [header-key (or (some-> resp (find-header header-name) key) header-name)]
    (update-in resp [:headers header-key] #(apply f % args))))

(defn charset
  "Returns an updated response with the supplied charset added to the
  content-type header."
  [resp ?charset]
  (update-header resp "content-type"
                 (fn [?content-type]
                   (-> (or ?content-type "text/plain")
                       (s/replace #";\s*charset=[^;]*" "")
                       (str "; charset=" ?charset)))))

(defn- cookie
  [k v {:keys [domain expires]}]
  (s/replace
   (cond-> (str k "=" v)
     domain  (str "; domain=" domain ";")
     expires (str "; expires=" expires ";"))
   ";;" ";"))

(defn set-cookie
  "Sets a cookie on the response."
  [resp {:keys [k v] :as opts}]
  (update-in resp [:multiValueHeaders "set-cookie"]
             (fn [xv]
               (vec (conj xv (cookie k v (dissoc opts :k :v)))))))

(defn origin
  [resp ?origin]
  (header resp "access-control-allow-origin" ?origin))

(defn credentials
  [resp ?creds]
  (header resp "access-control-allow-credentials" ?creds))
