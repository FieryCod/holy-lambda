(ns fierycod.holy-lambda.util-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.util :as u]
   [fierycod.holy-lambda.core :as h]))

(h/deflambda call-lambda-01-valid-internal
  [{:keys [event]}]
  event)

(t/deftest call-lambda-fn-test
  (t/testing "should take the lambda and invoke it passing correct result"
    (t/is (= {:ok "OK"}
             (u/call #'call-lambda-01-valid-internal {:event {:ok "OK"}})))))
    (t/is (= true
             (fn? (u/call #'call-lambda-01-valid-internal))))
