(ns fierycod.holy-lambda.core-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.core :as h]))

(h/deflambda call-lambda-01-valid-internal [{:keys [event]}] event)

(h/deflambda call-lambda-01-valid-internal
  [{:keys [event]}]
  event)

(defn catch-macro
  [macro]
  (try
    (macroexpand `~macro)
    (catch Exception e
      (.getMessage (.getCause e)))))

(t/deftest parse-of-deflambda-test
  (t/testing "Deflambda attributes should correctly be converted into map of attrs"
    (t/is (= "First argument to deflambda must be a symbol" (catch-macro `(h/deflambda ""))))
    (t/is (= "Syntax error at (fierycod.holy-lambda.core-test/err)" (catch-macro `(h/deflambda ns err))))
    (t/is (= "Mixin must be given before argument list" (catch-macro `(h/deflambda ns [] ~'<))))
    (t/is (= {:lname (symbol "Example"),
              :doc "Docstring",
              :mixin {:some-mixin ""}}
             (select-keys (#'fierycod.holy-lambda.core/>parse-deflambda
                           [(symbol "Example")
                            "Docstring"
                            (symbol "<")
                            {:some-mixin ""}
                            [(symbol "request")]
                            `(println (symbol "request"))])
                          [:lname :doc :mixin])))))

(t/deftest merge-mixins-test
  (t/testing "merging mixins should work"
    (t/is (= {:interceptors []} (h/merge-mixins {} {})))
    (t/is (= {:interceptors []} (h/merge-mixins {:interceptors []} {:interceptors []})))
    (t/is (= {:interceptors []} (h/merge-mixins {:interceptors nil} {:interceptors []})))
    (t/is (= {:interceptors []} (h/merge-mixins {:interceptors []} {:interceptors nil})))
    (t/is (= {:interceptors [1 2]} (h/merge-mixins {:interceptors [1 2]} {:interceptors nil})))
    (t/is (= {:interceptors [1 2]} (h/merge-mixins {:interceptors nil} {:interceptors [1 2]})))
    (t/is (= {:interceptors [1 1 2]} (h/merge-mixins {:interceptors [1]} {:interceptors [1 2]})))))
