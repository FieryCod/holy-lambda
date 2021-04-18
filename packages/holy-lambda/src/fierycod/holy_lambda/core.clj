(ns fierycod.holy-lambda.core
  "Integrates the Clojure code with two different runtimes: Java Lambda Runtime, Native Provided Runtime.
  The former is the Official Java Runtime for AWS Lambda which is well tested and works perfectly fine, but it's rather slow due to cold starts whereas,
  the latter is a custom [runtime](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html) integrated within the framework.
  It's a significantly faster than the Java runtime due to the use of GraalVM."
  (:require
   [fierycod.holy-lambda.interceptor :as i]
   [fierycod.holy-lambda.java-runtime :as jruntime]
   [fierycod.holy-lambda.util :as u])
  (:import
   [clojure.lang IPersistentMap]
   [com.amazonaws.services.lambda.runtime Context]
   [java.io InputStream OutputStream]))

(defn- wrap-lambda
  [gmethod-sym mixin lambda gclass]
  `(do
     ~gclass
     (defn ~gmethod-sym
       ;; Arity used for testing and native runtime invocation
       ([request#]
        (#'fierycod.holy-lambda.interceptor/process-interceptors
         ~mixin
         (~lambda
          (#'fierycod.holy-lambda.interceptor/process-interceptors ~mixin
                                                                   request#
                                                                   :enter))
         :leave))

       ;; Arity used for Java runtime
       ([this# ^InputStream in# ^OutputStream out# ^Context ctx#]
        ;; TODO: Move me to java runtime
        (try
          (let [event# (#'fierycod.holy-lambda.util/in->edn-event in#)
                context# (#'fierycod.holy-lambda.java-runtime/java-ctx-object->ctx-edn ctx# (#'fierycod.holy-lambda.util/envs))
                response# (#'fierycod.holy-lambda.util/response->bytes
                           (#'fierycod.holy-lambda.interceptor/process-interceptors
                            ~mixin
                            (~lambda (#'fierycod.holy-lambda.interceptor/process-interceptors ~mixin
                                                                                              {:event event#
                                                                                               :ctx context#}
                                                                                              :enter))
                            :leave))]
            (.write out# ^"[B" response#))
          (catch Exception error#
            (println "[Holy Lambda] Exception during request handling" error#))
          (finally
            (.close out#)))))))

(defn- fn-body?
  [form]
  (when (vector? (first form))
    (if (= '< (second form))
      (throw (IllegalArgumentException. "Mixin must be given before argument list"))
      true)))

(defn- >parse-deflambda
  ":name  :doc?  <? :mixin* :body+
   symbol string <  expr   fn-body"
  [attrs]
  (when-not (instance? clojure.lang.Symbol (first attrs))
    (throw (IllegalArgumentException. "First argument to deflambda must be a symbol")))
  (loop [res  {}
         xs   attrs
         mode nil]
    (let [x    (first xs)
          anext (next xs)]
      (cond
        (and (empty? res) (symbol? x)) (recur {:lname x} anext nil)
        (fn-body? xs)        (assoc res :bodies (list xs))
        (string? x)          (recur (assoc res :doc x) anext nil)
        (= '< x)             (recur res anext :mixin)
        (= mode :mixin) (recur (assoc res :mixin x) anext nil)
        :else (throw (IllegalArgumentException. (str "Syntax error at " xs)))))))

(defmacro deflambda
  "Convenience macro for generating defn alike lambdas."
  {:arglists '([name doc-string? <mixin-sign? mixin? [request] fn-body])
   :added "0.0.1"}
  [& attrs]
  (let [{:keys [mixin lname bodies doc]} (>parse-deflambda attrs)
        arglist (mapv (fn [[arglist & _body]] arglist) bodies)
        lname (with-meta lname {:doc doc
                                :arglists `(quote ~arglist)})
        prefix (str "PRVL" lname "--")
        gmethod-sym (symbol (str prefix "handleRequest"))
        gfullname (symbol (str (ns-name *ns*) "." lname))
        gclass (jruntime/gen-lambda-class-with-prefix prefix gfullname)
        lambda `(fn ~@bodies)]

    `(do ~(wrap-lambda gmethod-sym mixin lambda gclass)
          (def ~lname ~gmethod-sym))))

(defn merge-mixins
  "Merges multiple mixins properties"
  [& mixins]
  (reduce (fn [mixin1 mixin2]
            (-> mixin1
                (update :interceptors (comp vec (fnil concat [])) (or (:interceptors mixin2) []))))
          mixins))

