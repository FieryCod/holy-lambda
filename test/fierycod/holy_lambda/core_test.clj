(ns fierycod.holy-lambda.core-test
  (:require
   [clojure.test :refer :all]
   [fierycod.holy-lambda.core :refer :all]))

(deftest extract-event-fn
  (testing "should read json string and transform it to clojure datastructure"
    ((#'fierycod.holy-lambda.core/extract-event escaped-event-str))
    (is (= 0 1))))
