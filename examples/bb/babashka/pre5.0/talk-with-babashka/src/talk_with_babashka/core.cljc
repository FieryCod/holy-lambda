(ns talk-with-babashka.core
  (:gen-class)
  (:require
   [babashka.pods :as pods]
   [fierycod.holy-lambda.response :as hr]
   [fierycod.holy-lambda.native-runtime :as native]
   [fierycod.holy-lambda.core :as h]
   [clojure.java.io :as io])
  (:import
   [java.security MessageDigest]))

;; Let's load a babashka AWS pod first
(pods/load-pod 'org.babashka/aws "0.0.6")
(require '[pod.babashka.aws :as aws])

(def polly-client (aws/client {:api :polly :region "eu-central-1"}))
(def s3-client    (aws/client {:api :s3    :region "eu-central-1"}))

(defn synthesize-speech!
  [text]
  (aws/invoke polly-client {:op      :SynthesizeSpeech
                            :request {:OutputFormat "mp3"
                                      :Text         text
                                      :TextType     "text"
                                      :Engine       "neural"
                                      :VoiceId      "Matthew"}}))


(defn s3-put-speech!
  [{:keys [key speech bucket]}]
  (aws/invoke s3-client {:op      :PutObject
                         :request {:Body          (:AudioStream speech)
                                   :Key           key
                                   :ACL           "public-read"
                                   :Bucket        bucket
                                   :ContentType   "audio/mp3"}}))

(defn sha256
  [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))

(defn synthesis-html
  [url]
  (hr/html
   (format
"
<div>
  <span class=\"has-text-success\"> Audio response:</span>
  <audio controls>
    <source src=\"%s\" type=\"audio/mpeg\">
  </audio>
<div>
"
    url)))

(defn exists-in-s3?
  [{:keys [key bucket]}]
  (let [result (aws/invoke s3-client {:op      :HeadObject
                                     :request {:Key    key
                                               :Bucket bucket}})]
    (if-let [anomaly (:cognitect.anomalies/category result)]
      (if (= anomaly :cognitect.anomalies/not-found)
        false
        (throw (ex-info "Error when checking key existance in s3" {:anomaly anomaly})))
      true)))

(h/deflambda SpeechSynthesis
  [{:keys [event]
    {:keys [envs]} :ctx}]
  (let [{:keys [text]} (:body event)
        bucket (get envs "S3_BUCKET_NAME" nil)
        text-key (str (some-> text sha256) ".mp3")
        url (str "https://" bucket ".s3.amazonaws.com/" text-key)]
    (if-not text
      (hr/text "Text has not been defined")
      (if (exists-in-s3? {:key text-key
                          :bucket bucket})
        (synthesis-html url)
        (if-let [err (:Error (s3-put-speech! {:key text-key
                                              :bucket bucket
                                              :speech (synthesize-speech! text)}))]
          (hr/html (format "<span class=\"has-text-danger\">%s</span>" (:Message err)))
          (synthesis-html url))))))


(h/deflambda SpeechSynthesisUI
  [_]
  (hr/html (slurp "talk_with_babashka/index.html")))

(native/entrypoint [#'SpeechSynthesis #'SpeechSynthesisUI])

(comment
  (do
    (defn speech-to-file!
      [speech file]
      (when-let [stream (:AudioStream speech)]
        (io/copy stream (io/file (str "resources/audio/" file ".mp3")))))

    (def speech (aws/invoke polly-client {:op :SynthesizeSpeech
                                          :request {:OutputFormat "mp3"
                                                    :Text "Clojure is awesome!"
                                                    :TextType "text"
                                                    :Engine "neural"
                                                    :VoiceId "Matthew"}}))
    (s3-put-speech! {:key "clojure-is-awesome.mp3"
                     :bucket "talk-with-babashka-2895a-st-speechsynthesisbucket-6046alpm3tim"
                     :speech speech})
    (speech-to-file! speech "clojure")))
