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
            (assoc response :x "x"))})

(h/deflambda Test2 <
  {:interceptors [Interceptor2]}
  [request]
  request)

(t/deftest simple-interceptor-1
  (t/testing ":enter should decorate request"
    (= {:x "x", :fierycod.holy-lambda.interceptor/interceptors {:complete {:leave ["Interceptor2"]}}}
       (u/call #'Test2 {})))

  (t/testing ":leave should decorate response"
    (t/is (= {:x "x", :fierycod.holy-lambda.interceptor/interceptors {:complete {:leave ["Interceptor2"]}}}
             (u/call #'Test2 {})))))
