SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

.PHONY: all check-service clean deploy deploy-native destroy destroy-bucket dry-api dry-api-native local-test make-bucket
BUCKET_NAME=hello-lambda-$$(whoami)
STACK_NAME=hello-lambda
APP_REGION=eu-central-1
PWD=$$(pwd)

ifeq ($(UNAME_S),Linux)
	native_image_cmd=native-image -jar target/output.jar --report-unsupported-elements-at-runtime --no-fallback --enable-url-protocols=http,https --no-server  --initialize-at-build-time
else
	native_image_cmd=docker run -v ${PWD}:/project:Z -it fierycod/graalvm-native-image:latest bash -c "cd /project && native-image -jar target/output.jar --report-unsupported-elements-at-runtime --no-fallback --verbose --enable-url-protocols=http,https --no-server  --initialize-at-build-time --initialize-at-build-time=org.apache.log4j.CategoryKey --trace-object-instantiation=java.lang.Thread"
endif

all: deploy-native

check-service:
	@(nc -z localhost 3000) || echo "AWS API service is not running: run 'make dry-api' or 'make dry-api-native."

clean:
	-rm -rf server target/ packaged.yml packaged-native.yml output latest.zip

deploy: packaged.yml
	sam deploy --template-file packaged.yml        --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

deploy-native: packaged-native.yml
	sam deploy --template-file packaged-native.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

destroy:
	aws cloudformation delete-stack --stack-name $(STACK_NAME) --region $(APP_REGION)

destroy-bucket:
	aws s3 rb s3://$(BUCKET_NAME) --force --region $(APP_REGION)

dry-api: target/output.jar
	sam local start-api --template template.yml

dry-api-native: latest.zip
	sam local start-api --template template-native.yml

local-test: check-service
	curl "http://127.0.0.1:3000/"
	curl "http://127.0.0.1:3000/bye"

make-bucket:
	aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)

latest.zip: output
	zip -j latest bootstrap output

output: target/output.jar
	${native_image_cmd}

packaged.yml: target/output.jar make-bucket
	sam package --template-file template.yml        --output-template-file packaged.yml        --s3-bucket $(BUCKET_NAME) --s3-prefix "hello-lambda"

packaged-native.yml: latest.zip make-bucket
	sam package --template-file template-native.yml --output-template-file packaged-native.yml --s3-bucket $(BUCKET_NAME) --s3-prefix "hello-lambda"

target/output.jar: src/hello_lambda/core.clj
	lein uberjar
