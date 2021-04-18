(ns fierycod.holy-lambda.native-runtime-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.core :as h]
   [fierycod.holy-lambda.native-runtime :as native]))

(h/deflambda call-lambda-01-valid-internal [{:keys [event]}] event)

(t/deftest entrypoint-test
  (t/testing "should properly generate main fn"
    (native/entrypoint [#'call-lambda-01-valid-internal])
    (t/is (= {"fierycod.holy-lambda.native-runtime-test.call-lambda-01-valid-internal"
              #'fierycod.holy-lambda.native-runtime-test/call-lambda-01-valid-internal}
             PRVL_ROUTES))))
