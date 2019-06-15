(ns fierycod.holy-lambda.impl.agent-test
  (:require
   [fierycod.holy-lambda.core :as h]
   [fierycod.holy-lambda.impl.agent :as a]
   [clojure.test :as t]))

(t/deftest native-agents-files->payloads-map-fn-test
  (t/testing "should generate the map from pairs [HandlerName, [{:event sth, :context sth, :envs sth, :path sth}, ...]]"
    (t/is (= (#'fierycod.holy-lambda.impl.agent/native-agents-files->payloads-map)
             '({:name "fierycod.holy-lambda.core.HolyLambda",
               :event {:lambda2 2},
               :context {},
               :envs {},
               :path "resources/native-agents-payloads/2.json"}
              {:name "fierycod.holy-lambda.core.HolyLambda",
               :event {},
               :context {},
               :envs {},
               :path "resources/native-agents-payloads/1.json"}
              {:name "fierycod.holy-lambda.core.NewLambda",
               :event {:msg "new-lambda"},
               :context {:ctx "CTX"},
               :envs {},
               :path "resources/native-agents-payloads/3.json"})
             ))))

(t/deftest call-lambdas-with-agent-payloads-fn-test
  (t/testing "should call all lambdas with corresponding payloads and report on each step"
    (t/is (= (a/call-lambdas-with-agent-payloads h/call)
             ))))
