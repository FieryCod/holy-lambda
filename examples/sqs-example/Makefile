SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

.PHONY: install-holy-lambda native-dry-run compile dry-run native-compile native-pack native-deploy native-destroy logs-tail
BUCKET_NAME=holy-lambda--sqs-example
STACK_NAME=holy-lambda--sqs-example
APP_REGION=eu-central-1
LAMBDA_NAME=ReceiveStringLambdaFunction
EVENT_PAYLOAD_FILE=./sample-receive-event.json

make-bucket:
	@aws s3 mb s3://$(BUCKET_NAME)

destroy-bucket:
	@aws s3 rb s3://$(BUCKET_NAME) --force --region $(APP_REGION)

install-holy-lambda:
	@bash -c "cd ../../ && lein install"

compile:
	@lein uberjar

dry-api:
	@sam local start-api --skip-pull-image

gen-native-configuration:
	@choly graal -e "java -agentlib:native-image-agent=config-output-dir=resources/native-configuration -Dexecutor=native-agent -jar target/sqs-example.jar" \
	 	              "--env-file=resources/envs.list"

native-compile:
	@choly graal -e "native-image -jar target/output.jar \
	          --report-unsupported-elements-at-runtime \
            -H:ConfigurationFileDirectories=resources/native-configuration \
						--no-fallback \
						--enable-url-protocols=http,https \
	 					--no-server -J-Xmx3G -J-Xms3G \
	 					--initialize-at-build-time \
	 					--allow-incomplete-classpath \
            --enable-all-security-services"
	@mv -f output server
	@zip -j latest resources/bootstrap server resources/native-deps/*
	@mv latest.zip resources/
	@rm -Rf server

native-sqs-invoke:
	@sam local invoke $(LAMBDA_NAME) --template ./resources/native-template.yml --skip-pull-image -e $(EVENT_PAYLOAD_FILE) -n resources/sam-envs.json

sqs-invoke:
	@sam local invoke $(LAMBDA_NAME) --template ./template.yml --skip-pull-image -e $(EVENT_PAYLOAD_FILE)

pack:
	@sam package --template-file ./template.yml --output-template-file packaged.yaml --s3-bucket $(BUCKET_NAME) --s3-prefix "sqs-example-latest"

deploy:
	@sam deploy --template-file ./packaged.yaml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

native-pack:
	@sam package --template-file ./resources/native-template.yml --output-template-file resources/native-packaged.yaml --s3-bucket $(BUCKET_NAME) --s3-prefix "sqs-example-latest"

native-deploy:
	@sam deploy --template-file ./resources/native-packaged.yaml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

native-destroy:
	@aws cloudformation delete-stack --stack-name $(STACK_NAME) --region $(APP_REGION)

logs-tail:
	sam logs -n $(LAMBDA_NAME) --stack-name $(STACK_NAME) -t
