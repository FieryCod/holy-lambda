(ns fierycod.holy-lambda.response-test
  (:require
   [clojure.test :as t]
   [fierycod.holy-lambda.response :as r]))

(t/deftest response-utils-test
  (t/testing "bad-request should return valid response"
    (t/is (= {:statusCode 400, :headers {}, :body {}}
             (r/bad-request {})))
    (t/is (= {:statusCode 400, :headers {}, :body nil}
             (r/bad-request nil))))

  (t/testing "redirect should provide AWS compatible redirection format"
    (t/is (= {:statusCode 302, :headers {"Location" "https://www.google.com/"}, :body nil}
             (r/redirect "https://www.google.com/")))
    (t/is (= {:statusCode 301, :headers {"Location" "https://www.google.com/"}, :body nil}
             (r/redirect "https://www.google.com/" 301))))

  (t/testing "setting the headers should work"
    (t/is
     (= {:headers {"Content-Type" "Some-Content-Type", "Header1" "Value1"}}
        (-> {}
            (r/header "Content-Type" "Some-Content-Type")
            (r/header "Header1" "Value1")))))

  (t/testing "finding from headers should work"
    (t/is
     (= ["Content-Type" "Some-Content-Type"]
        (-> {}
            (r/header "Content-Type" "Some-Content-Type")
            (r/find-header "Content-Type")))))

  (t/testing "finding from headers should work"
    (t/is
     (= {:headers {"Content-Type" "application/json"}}
        (-> {}
            (r/content-type "application/json")))))

  (t/testing "not found template should work"
    (t/is
     (= {:statusCode 404, :headers {}, :body nil}
        (r/not-found nil))))

  (t/testing "json template should work"
    (t/is
     (= {:statusCode 200, :headers {"Content-Type" "application/json; charset=utf-8"}, :body {:message "message"}}
        (r/json {:message "message"}))))

  (t/testing "check static-status-codes"
    (t/is
     (= {:moved-permanently 301, :found 302, :see-other 303, :temporary-redirect 307, :permanent-redirect 308}
        r/redirect-status-codes)))

  (t/testing "should return the get header"
    (t/is
     (= "application/json"
        (r/get-header {:headers {"Content-Type" "application/json"}} "Content-Type"))))

  (t/testing "should update the header"
    (t/is
     (= {:headers {"Content-Type" "application/jsonxx"}}
        (r/update-header {:headers {"Content-Type" "application/json"}} "Content-Type" (fn [x] (str x "xx"))))))

  (t/testing "created template should work"
    (t/is
     (= {:statusCode 201, :headers {"Location" "https://www.google.com/"}, :body nil}
        (r/created "https://www.google.com/" nil))))

  (t/testing "set-cookie should use :multiValueHeaders"
    (t/is
     (= {:multiValueHeaders {"Set-Cookie" ["Key=Value; domain=https://localhost:3000;"]}}
        (r/set-cookie {}
                      {:k "Key"
                       :v "Value"
                       :domain "https://localhost:3000"})))))
