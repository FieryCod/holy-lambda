(ns fierycod.holy-lambda.core-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.core :as h]))

(defn call-lambda-01-valid-internal [{:keys [event]}] event)

(t/deftest entrypoint-test
  (t/testing "should properly generate main fn"
    (h/entrypoint [#'call-lambda-01-valid-internal])
    (t/is (= {"fierycod.holy-lambda.core-test.call-lambda-01-valid-internal"
              #'fierycod.holy-lambda.core-test/call-lambda-01-valid-internal}
             PRVL_ROUTES))))
