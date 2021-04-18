# Getting started

## Dependencies
  You will need following things which you have to install on your own depending on your system.

  - Homebrew/Linuxbrew
  - Java 8
  - Docker, Docker Compose >= 1.13.1, 1.22.0

  After having all mentioned above dependencies just use the following commands:

  1. Install aws, aws-sam, leiningen and clojure
    ```
    brew tap aws/tap && brew install leiningen awscli aws-sam-cli clojure/tools/clojure
    ```

  2. Configure default AWS profile via `aws-cli`. This is necessary for making a bucket and deploying an application. If you just want to test holy-lambda on local then this step is not necessary, but you will be able to use limited set of commands.

   ```
   aws configure
   ```

## First project 

1. Create a new project with `lein new lambda-test` and `cd` to that directory. 
2. Create a new file called `Makefile` in your newly-created `lambda-test` directory with the following contents:

```
.PHONY: deploy-native dry-api-native make-bucket
BUCKET_NAME=lambda-test
STACK_NAME=lambda-test 
APP_REGION=eu-central-1 # or choose your preferred AWS region
PWD=$$(pwd)

native_image_cmd=docker run -v ${PWD}:/project -it ghcr.io/graalvm/graalvm-ce:latest bash -c "gu install native-image && cd /project && native-image -jar target/output.jar --report-unsupported-elements-at-runtime --no-fallback --verbose --enable-url-protocols=http,https --no-server  --initialize-at-build-time --initialize-at-build-time=org.apache.log4j.CategoryKey --trace-object-instantiation=java.lang.Thread"

deploy-native: packaged-native.yml
	sam deploy --template-file packaged-native.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

packaged-native.yml: latest.zip make-bucket
	sam package --template-file template-native.yml --output-template-file packaged-native.yml --s3-bucket $(BUCKET_NAME) --s3-prefix "lambda-test-lambda"

latest.zip: output
	zip $@ bootstrap output

output: target/output.jar
	${native_image_cmd}

target/output.jar: src/lambda-test_lambda/core.clj
	lein uberjar
```

3. Create a new file called `template-native.yml` in the same directory with the following contents:

```
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: "A test lambda function using using `holy-lambda` micro library"
Parameters:
  Runtime:
    Type: String
    Default: provided
  Timeout:
    Type: Number
    Default: 40
  MemorySize:
    Type: Number
    Default: 2000
Globals:
  Function:
    Runtime: !Ref 'Runtime'
    Timeout: !Ref 'Timeout'
    MemorySize: !Ref 'MemorySize'
Resources:
  GenerateNLNative:
    Type: AWS::Serverless::Function
    Properties:
      Handler: lambda-test.handlers.foo
      CodeUri: ./latest.zip
      FunctionName: Foo
      Events:
        Event:
          Type: Api
          Properties:
            Path: /foo
            Method: get
```

4. Create a new file called `handlers.clj` in your `src/lambda_test` directory with the following contents:

```
(ns lambda-test.handlers
  (:gen-class)
  (:require
    [fierycod.holy-lambda.core :as h]
	[lambda-test.core :refer [foo]]))

(h/deflambda Foo
  [event context]
  (let [q (-> event :queryStringParameters :q)]
    {:statusCode 200
     :headers {"Content-Type" "application/json"}
     :body (foo q)
     :isBase64Encoded false}))
```
