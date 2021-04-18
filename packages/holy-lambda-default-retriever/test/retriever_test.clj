(ns fierycod.holy-lambda.retriever-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.retriever :as r]))

(t/deftest <-wait-for-response-test
  (t/testing "<-wait-for-response should wait for either map, future or promise"
    (t/is (= {} (r/<-wait-for-response {})))
    (t/is (= "str" (r/<-wait-for-response "str")))
    (t/is (= nil (r/<-wait-for-response nil)))
    (t/is (= "Hello World" (r/<-wait-for-response (future "Hello World"))))
    (t/is (= "Hello World" (r/<-wait-for-response (let [p (promise)] (future (deliver p "Hello World")) p))))))
