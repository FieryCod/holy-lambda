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

  (t/testing "response basic template"
    (t/is (= {:statusCode 200, :headers {}, :body {}}
             (r/response {}))))

  (t/testing "status basic template"
    (t/is (= {:statusCode 200, :headers {}, :body nil}
             (r/status 200)))
    (t/is (= {:statusCode 200, :body "hello"}
             (r/status {:body "hello"} 200))))

  (t/testing "redirect should provide AWS compatible redirection format"
    (t/is (= {:statusCode 302, :headers {"location" "https://www.google.com/"}, :body nil}
             (r/redirect "https://www.google.com/")))
    (t/is (= {:statusCode 301, :headers {"location" "https://www.google.com/"}, :body nil}
             (r/redirect "https://www.google.com/" 301))))

  (t/testing "setting the headers should work"
    (t/is
     (= {:headers {"content-type" "Some-Content-Type", "Header1" "Value1"}}
        (-> {}
            (r/header "content-type" "Some-Content-Type")
            (r/header "Header1" "Value1")))))

  (t/testing "finding from headers should work"
    (t/is
     (= ["content-type" "Some-Content-Type"]
        (-> {}
            (r/header "content-type" "Some-Content-Type")
            (r/find-header "content-type")))))

  (t/testing "finding from headers should work"
    (t/is
     (= {:headers {"content-type" "application/json"}}
        (-> {}
            (r/content-type "application/json")))))

  (t/testing "not found template should work"
    (t/is
     (= {:statusCode 404, :headers {}, :body nil}
        (r/not-found nil))))

  (t/testing "json template should work"
    (t/is
     (= {:statusCode 200, :headers {"content-type" "application/json"}, :body {:message "message"}}
        (r/json {:message "message"}))))

  (t/testing "check static-status-codes"
    (t/is
     (= {:moved-permanently 301, :found 302, :see-other 303, :temporary-redirect 307, :permanent-redirect 308}
        r/redirect-status-codes)))

  (t/testing "should return the get header"
    (t/is
     (= "application/json"
        (r/get-header {:headers {"content-type" "application/json"}} "content-type")))
    (t/is (= "ex" (try (r/get-header {:headers {"content-type" "something"}} nil) (catch Exception _ "ex"))))
    (t/is
     (= "something"
        (r/get-header {:headers {"content-type" "something"}} "content-type"))))

  (t/testing "charset should work"
    (t/is
     (=
      {:headers {"content-type" "application/json; charset=utf-8"}}
      (r/charset {:headers {"content-type" "application/json"}} "utf-8")))

    (t/is
     (=
      {:headers {"content-type" "text/plain; charset=utf-8"}}
      (r/charset {:headers {"content-type" "text/plain"}} "utf-8"))))

  (t/testing "should update the header"
    (t/is
     (= {:headers {"content-type" "application/jsonxx"}}
        (r/update-header {:headers {"content-type" "application/json"}} "content-type" (fn [x] (str x "xx"))))))

  (t/testing "created template should work"
    (t/is
     (= {:statusCode 201, :headers {"location" "https://www.google.com/"}, :body nil}
        (r/created "https://www.google.com/")))

    (t/is
     (= {:statusCode 201, :headers {"location" "https://www.google.com/"}, :body nil}
        (r/created "https://www.google.com/" nil))))

  (t/testing "text template should-work"
    (t/is
     (= {:statusCode 200, :headers {"content-type" "text/plain; charset=utf-8"}, :body "Hello World"}
        (r/text "Hello World"))))

  (t/testing "set-cookie should use :multiValueHeaders"
    (t/is
     (= {:multiValueHeaders {"set-cookie" ["Key=Value; domain=https://localhost:3000;"]}}
        (r/set-cookie {}
                      {:k "Key"
                       :v "Value"
                       :domain "https://localhost:3000"})))
    (t/is
     (= {:multiValueHeaders {"set-cookie" ["Key=Value; domain=https://localhost:3000; expires=Tomorrow;"]}}
        (r/set-cookie {}
                      {:k "Key"
                       :v "Value"
                       :domain "https://localhost:3000"
                       :expires "Tomorrow"}))))

  (t/testing "origin should work"
    (t/is
     (= {:headers {"access-control-allow-origin" "*"}} (r/origin {} "*"))))

  (t/testing "credentials should work"
    (t/is
     (= {:headers {"access-control-allow-credentials" "true"}} (r/credentials {} true)))

    (t/is
     (= {:headers {"access-control-allow-credentials" "false"}} (r/credentials {} false))))

  (t/testing "html response should work"
    (t/is
     (= {:statusCode 200, :headers {"content-type" "text/html; charset=utf-8"}, :body ""}
        (r/html "")))))
