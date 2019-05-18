(ns fierycod.holy-lambda.core-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.core :as h]))

(t/deftest call-lambda-fn-test
  (t/testing "should take the lambda and invoke it passing correct result"
    (h/deflambda call-lambda-01-valid-internal [event _] event)
    (t/is (= (h/call #'call-lambda-01-valid-internal {:ok "OK"} {}) {:ok "OK"})))

  (t/testing "should take the lambda, invoke it and throw AssertionError due to incorrect arity"
    (h/deflambda call-lambda-02-invalid-internal [event _] event)
    (t/is (thrown? java.lang.AssertionError (h/call #'call-lambda-02-invalid-internal {:ok "OK"})))))

;; TODO Write more tests
