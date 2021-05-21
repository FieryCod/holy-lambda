(ns fierycod.holy-lambda.interceptor-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.util :as u]
   [fierycod.holy-lambda.interceptor :as i]
   [fierycod.holy-lambda.core :as h]))

(i/definterceptor Interceptor1
  {:enter (fn [request]
            (assoc request :x "x"))})

(h/deflambda Test1 <
  {:interceptors [Interceptor1]}
  [request]
  request)

(i/definterceptor Interceptor2
  {:leave (fn [response]
            (assoc response :y "y"))})

(h/deflambda Test2 <
  {:interceptors [Interceptor2]}
  [request]
  request)


(i/definterceptor Interceptor3
  {:enter (fn [request]
            (assoc request :xy "xy"))
   :leave (fn [response]
            (assoc response :z "z"))})

(h/deflambda Test3 <
  {:interceptors [Interceptor1 Interceptor2 Interceptor3]}
  [request]
  request)

(t/deftest simple-interceptor-1
  (t/testing ":enter should decorate request"
    (t/is (= {:x "x", :fierycod.holy-lambda.interceptor/interceptors {:complete {:enter ["Interceptor1"]}}}
             (u/call #'Test1 {}))))

  (t/testing ":leave should decorate response"
    (t/is (= {:y "y", :fierycod.holy-lambda.interceptor/interceptors {:complete {:leave ["Interceptor2"]}}}
             (u/call #'Test2 {}))))

  (t/testing "multiple interceptors should work"
    (t/is (= {:x "x",
              :xy "xy",
              :y "y",
              :z "z",
              :fierycod.holy-lambda.interceptor/interceptors {:complete {:enter ["Interceptor1"
                                                                                 "Interceptor3"],
                                                                         :leave ["Interceptor3"
                                                                                 "Interceptor2"]}}}
             (u/call #'Test3 {}))))
  )
