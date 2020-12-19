SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

.PHONY: install-holy-lambda native-dry-run compile dry-run native-compile native-pack native-deploy native-destroy logs-tail clean destroy
BUCKET_NAME=holy-lambda--hello-lambda
STACK_NAME=holy-lambda--hello-lambda
APP_REGION=eu-central-1
LAMBDA_NAME=HelloLambdaFunction

make-bucket:
	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)

destroy-bucket:
	@aws s3 rb s3://$(BUCKET_NAME) --force --region $(APP_REGION)

install-holy-lambda:
	@bash -c "cd ../../ && lein install"

target/hello-lambda-0.1.0-SNAPSHOT.jar: src/hello_lambda/core.clj
	@lein uberjar

dry-api:
	@sam local start-api --skip-pull-image

native-compile: output
	mv -f output server
	zip -j latest resources/bootstrap server
	mv latest.zip resources/
	rm -Rf server

output: target/hello-lambda-0.1.0-SNAPSHOT.jar
	docker run -v /Users/ekoontz/holy-lambda/examples/hello-lambda/:/project:Z -it fierycod/graalvm-native-image:latest bash -c "cd /project && native-image -jar target/output.jar --report-unsupported-elements-at-runtime -H:ConfigurationFileDirectories=resources/native-configuration --no-fallback --enable-url-protocols=http,https --no-server  --initialize-at-build-time "

native-dry-api:
	@sam local start-api --template ./resources/native-template.yml --skip-pull-image

pack: compile make-bucket
	@sam package --template-file ./template.yml --output-template-file packaged.yml --s3-bucket $(BUCKET_NAME) --s3-prefix "hello-lambda-latest"

deploy: pack
	@sam deploy --template-file ./packaged.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

native-pack: native-compile make-bucket
	@sam package --template-file ./resources/native-template.yml --output-template-file resources/native-packaged.yml --s3-bucket $(BUCKET_NAME) --s3-prefix "hello-lambda-latest"

native-deploy: native-pack
	@sam deploy --template-file ./resources/native-packaged.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

destroy:
	@aws cloudformation delete-stack --stack-name $(STACK_NAME) --region $(APP_REGION)

logs-tail:
	sam logs -n $(LAMBDA_NAME) --stack-name $(STACK_NAME) -t

native-invoke:
	sam local invoke $(LAMBDA_NAME) --template ./resources/native-template.yml --skip-pull-image -e $(EVENT_PAYLOAD_FILE)

clean: destroy
	-rm -rf target/
	-rm packaged.yml
	-rm resources/native-packaged.yml
