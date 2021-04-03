(ns fierycod.holy-lambda.core-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.core :as h]))

(t/deftest call-lambda-fn-test
  (t/testing "should take the lambda and invoke it passing correct result"
    (h/deflambda call-lambda-01-valid-internal [{:keys [event]}] event)
    (t/is (= {:ok "OK"}
             (h/call #'call-lambda-01-valid-internal {:event {:ok "OK"}})))))
