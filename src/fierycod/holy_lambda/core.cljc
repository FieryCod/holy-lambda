(ns fierycod.holy-lambda.core
  "Integrates the Clojure functions with supported runtimes:
   - Clojure Lambda Runtime,
   - Native Provided Runtime,
   - Babashka runtime.

   See the docs for more info.")

(defn- wrap-lambda
  [lname mixin lambda]
  `(defn ~lname
     [request#]
     (~lambda request#)))

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
        (and (empty? res)    (symbol? x)) (recur {:lname x} anext nil)
        (fn-body? xs)        (do
                               (when (or (not (nil? (second (first xs))))
                                         (nil? (ffirst xs)))
                                 (throw (ex-info "Incorrect deflambda definition. Lambda takes only one argument [request] that consist of {event, ctx}" {})))
                               (assoc res :bodies (list xs)))
        (string? x)          (recur (assoc res :doc x) anext nil)
        (= '< x)             (recur res anext :mixin)
        (= mode :mixin)      (recur (assoc res :mixin x) anext nil)
        :else                (throw (ex-info (str "Syntax error at " xs) {}))))))

(defmacro deflambda
  "Convenience macro for generating defn alike lambdas.

  **Usage**:
  ```
  (h/deflambda ExampleLambda
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

