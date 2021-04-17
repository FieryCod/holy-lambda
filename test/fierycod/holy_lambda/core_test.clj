(ns fierycod.holy-lambda.core-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.core :as h]))

(h/deflambda call-lambda-01-valid-internal [{:keys [event]}] event)

(t/deftest parse-of-deflambda-test
  (t/testing "Deflambda attributes should correctly be converted into map of attrs"
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

(t/deftest call-lambda-fn-test
  (t/testing "should take the lambda and invoke it passing correct result"
    (t/is (= {:ok "OK"}
             (h/call #'call-lambda-01-valid-internal {:event {:ok "OK"}})))))

