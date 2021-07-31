(ns fierycod.holy-lambda.core
  "Integrates the Clojure code with two different runtimes: Java Lambda Runtime, Native Provided Runtime.
  The former is the Official Java Runtime for AWS Lambda which is well tested and works perfectly fine, but it's rather slow due to cold starts whereas,
  the latter is a custom [runtime](https://docs.aws.amazon.com/lambda/latest/dg/runtimes-custom.html) integrated within the framework.
  It's a significantly faster than the Java runtime due to the use of GraalVM."
  (:require
   [fierycod.holy-lambda.interceptor :as i]))

(defn- wrap-lambda
  [lname mixin lambda]
  `(defn ~lname
     ;; Arity used for testing and native runtime invocation
     [request#]
     (#'fierycod.holy-lambda.interceptor/process-interceptors
      ~mixin
      (~lambda
       (#'fierycod.holy-lambda.interceptor/process-interceptors ~mixin
                                                                request#
                                                                :enter))
      :leave)))

(defn- fn-body?
  [form]
  (when (vector? (first form))
    (if (= '< (second form))
      (throw (ex-info "Mixin must be given before argument list" {}))
      true)))

(defn- >parse-deflambda
  ":name  :doc?  <? :mixin* :body+
   symbol string <  expr   fn-body"
  [attrs]
  (when-not (symbol? (first attrs))
    (throw (ex-info "First argument to deflambda must be a symbol" {})))
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
        :else (throw (ex-info (str "Syntax error at " xs) {}))))))

(defmacro deflambda
  "Convenience macro for generating defn alike lambdas.

  Usage:
  ```
  (h/deflambda ExampleLambda
    \"I can run on both Java and Native...\"
    [{:keys [event ctx]}]
    (hr/text \"Hello world\"))
  ```"
  {:arglists '([name doc-string? <mixin-sign? mixin? [request] fn-body])
   :added "0.0.1"}
  [& attrs]
  (let [{:keys [mixin lname bodies doc]} (>parse-deflambda attrs)
        arglist (mapv (fn [[arglist & _body]] arglist) bodies)
        lname (with-meta lname {:doc doc
                                :arglists `(quote ~arglist)})
        lambda `(fn ~@bodies)]
    (wrap-lambda lname mixin lambda)))
