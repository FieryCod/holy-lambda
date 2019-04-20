(ns basic-lambda.core-test
  (:require [clojure.test :refer :all]
            [basic-lambda.core :refer :all]))

(def escaped-event-str "{\"httpMethod\": \"GET\", \"body\": null, \"resource\": \"\/\", \"requestContext\": {\"resourceId\": \"123456\", \"apiId\": \"1234567890\", \"resourcePath\": \"\/\", \"httpMethod\": \"GET\", \"requestId\": \"c6af9ac6-7b61-11e6-9a41-93e8deadbeef\r\n\", \"accountId\": \"123456789012\", \"stage\": \"prod\", \"identity\": {\"apiKey\": null, \"userArn\": null, \"cognitoAuthenticationType\": null, \"caller\": null, \"userAgent\": \"Custom User Agent String\", \"user\": null, \"cognitoId\r\nentityPoolId\": null, \"cognitoAuthenticationProvider\": null, \"sourceIp\": \"127.0.0.1\", \"accountId\": null}, \"extendedRequestId\": null, \"path\": \"\/\"}, \"queryStringParameters\": null, \"headers\": {\"Host\": \"localhost:300\r\n0\", \"Connection\": \"keep-alive\", \"Cache-Control\": \"max-age=0\", \"Upgrade-Insecure-Requests\": \"1\", \"User-Agent\": \"Mozilla\/5.0 (X11; Linux x86_64) AppleWebKit\/537.36 (KHTML, like Gecko) Chrome\/72.0.3626.96 Safari\/53\r\n7.36\", \"Accept\": \"text\/html,application\/xhtml+xml,application\/xml;q=0.9,image\/webp,image\/apng,*\/*;q=0.8\", \"Accept-Encoding\": \"gzip, deflate, br\", \"Accept-Language\": \"pl-PL,pl;q=0.9,en-US;q=0.8,en;q=0.7\", \"Cookie\r\n\": \"io=ud_5cAMCZKbZpjDPAAAA\", \"X-Forwarded-Proto\": \"http\", \"X-Forwarded-Port\": \"3000\"}, \"pathParameters\": null, \"stageVariables\": null, \"path\": \"\/\", \"isBase64Encoded\": false}")

(deftest a-test
  (testing "FIXME, I fail."))
    (is (= 0 1))
