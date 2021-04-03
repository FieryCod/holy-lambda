(ns fierycod.holy-lambda.impl.agent-test
  (:require
   [fierycod.holy-lambda.core :as h]
   [fierycod.holy-lambda.impl.agent :as agent]
   [clojure.test :as t]))

(h/deflambda HolyLambda
  [request]
  (println request))

(h/deflambda NewLambda
  [request]
  (println request))

(t/deftest agents-payloads->invoke-map-test
  (t/testing "should generate the map from pairs [HandlerName, [{:event sth, :context sth, :envs sth, :path sth}, ...]]"
    (t/is (= '({:request {:context {},
                          :event {}}
                :name "fierycod.holy-lambda.impl.agent-test.HolyLambda",
                :path "resources/native-agents-payloads/1.edn",
                :propagate false}
               {:request {:context {},
                          :event {:lambda2 2}}
                :name "fierycod.holy-lambda.impl.agent-test.HolyLambda",
                :path "resources/native-agents-payloads/2.edn",
                :propagate false}
               {:request {:context {:ctx "CTX"},
                          :event {:msg "new-lambda"}}
                :name "fierycod.holy-lambda.impl.agent-test.NewLambda",
                :path "resources/native-agents-payloads/3.edn",
                :propagate false})
             (#'fierycod.holy-lambda.impl.agent/agents-payloads->invoke-map)))))

(t/deftest routes->reflective-call!-test
  (t/testing "should call all lambdas with corresponding payloads and report on each step"
    (t/is (= "[Holy Lambda] Calling lambda fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from resources/native-agents-payloads/1.edn\n{:event {}, :context {}}\n[Holy Lambda] Succesfully called fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from resources/native-agents-payloads/1.edn\n[Holy Lambda] Calling lambda fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from resources/native-agents-payloads/2.edn\n{:event {:lambda2 2}, :context {}}\n[Holy Lambda] Succesfully called fierycod.holy-lambda.impl.agent-test.HolyLambda with payloads from resources/native-agents-payloads/2.edn\n[Holy Lambda] Calling lambda fierycod.holy-lambda.impl.agent-test.NewLambda with payloads from resources/native-agents-payloads/3.edn\n{:event {:msg new-lambda}, :context {:ctx CTX}}\n[Holy Lambda] Succesfully called fierycod.holy-lambda.impl.agent-test.NewLambda with payloads from resources/native-agents-payloads/3.edn\n[Holy Lambda] Succesfully called all the lambdas\n"
             (with-out-str (agent/routes->reflective-call! {"fierycod.holy-lambda.impl.agent-test.HolyLambda" #'HolyLambda,
                                                            "fierycod.holy-lambda.impl.agent-test.NewLambda" #'NewLambda}))))))
