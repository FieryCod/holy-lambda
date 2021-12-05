(ns fierycod.holy-lambda.agent-test
  (:require
   [fierycod.holy-lambda.core :as h]
   [fierycod.holy-lambda.agent :as agent]
   [fierycod.holy-lambda.util]
   [clojure.test :as t]))

(defn HolyLambda
  [request]
  (println request))

(defn NewLambda
  [request]
  (println request))

(t/deftest agents-payloads->invoke-map-test
  (t/testing "should generate the map from pairs [HandlerName, [{:event sth, :ctx sth, :path sth}, ...]]"
    (t/is (= '({:request {:ctx {},
                          :event {}}
                :name "fierycod.holy-lambda.impl.agent-test.HolyLambda",
                :path "1.edn",
                :propagate false}
               {:request {:ctx {},
                          :event {:lambda2 2}}
                :name "fierycod.holy-lambda.impl.agent-test.HolyLambda",
                :path "2.edn",
                :propagate false}
               {:request {:ctx {:ctx "CTX"},
                          :event {:msg "new-lambda"}}
                :name "fierycod.holy-lambda.impl.agent-test.NewLambda",
                :path "3.edn",
                :propagate false})
             (map (fn [m] (update m :path #(re-find #"(?<=.*)[A-Za-z0-9-_]*\..*" %)))
                  (#'fierycod.holy-lambda.agent/agents-payloads->invoke-map))))))

(t/deftest routes->reflective-call!-test
  (t/testing "should call all lambdas with corresponding payloads and report on each step"
    (t/is (= "[holy-lambda] Calling lambda fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from 1.edn\n{:event {}, :ctx {}}\n[holy-lambda] Succesfully called fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from 1.edn\n[holy-lambda] Calling lambda fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from 2.edn\n{:event {:lambda2 2}, :ctx {}}\n[holy-lambda] Succesfully called fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from 2.edn\n[holy-lambda] Calling lambda fierycod.holy-lambda.impl.agent-test.NewLambda with payloads from 3.edn\n{:event {:msg new-lambda}, :ctx {:ctx CTX}}\n[holy-lambda] Succesfully called fierycod.holy-lambda.impl.agent-test.NewLambda with payloads from 3.edn\n[holy-lambda] Succesfully called all the lambdas\n" (with-out-str (#'fierycod.holy-lambda.agent/routes->reflective-call! {"fierycod.holy-lambda.impl.agent-test.HolyLambda" #'HolyLambda,
                                                                                   "fierycod.holy-lambda.impl.agent-test.NewLambda" #'NewLambda})))))
  (with-redefs [fierycod.holy-lambda.util/exit! (fn [] (throw (Exception. "exited")))]
    (t/testing "should exit if lambda handler is not found"
      (t/is (= "exited" (try
                          (#'fierycod.holy-lambda.agent/routes->reflective-call!
                           {"fierycod.holy-lambda.impl.agent-test.HolyLambda" nil,
                            "fierycod.holy-lambda.impl.agent-test.NewLambda" #'NewLambda})
                          (catch Exception err
                            (.getMessage err)))))))
  )
