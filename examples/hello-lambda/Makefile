SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

.PHONY: install-holy-lambda native-dry-run compile dry-run native-compile native-pack native-deploy native-destroy logs-tail
BUCKET_NAME=holy-lambda--hello-lambda
STACK_NAME=holy-lambda--hello-lambda
APP_REGION=eu-central-1
LAMBDA_NAME=HelloLambdaFunction

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

native-compile:
	@rm -Rf latest.zip
	@fnative "-jar target/hello-lambda.jar \
						--report-unsupported-elements-at-runtime \
						--no-fallback \
						--enable-url-protocols=http,https \
						--no-server -J-Xmx3G -J-Xms3G \
						--initialize-at-build-time"
	@mv -f hello-lambda bootstrap
	@zip latest bootstrap

native-dry-api:
	@sam local start-api --template ./native-template.yml --skip-pull-image

pack:
	@sam package --template-file ./template.yml --output-template-file packaged.yaml --s3-bucket $(BUCKET_NAME) --s3-prefix "hello-lambda-latest"

deploy:
	@sam deploy --template-file ./packaged.yaml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

native-pack:
	@sam package --template-file ./native-template.yml --output-template-file native-packaged.yaml --s3-bucket $(BUCKET_NAME) --s3-prefix "hello-lambda-latest"

native-deploy:
	@sam deploy --template-file ./native-packaged.yaml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

native-destroy:
	@aws cloudformation delete-stack --stack-name $(STACK_NAME) --region $(APP_REGION)

logs-tail:
	sam logs -n $(LAMBDA_NAME) --stack-name $(STACK_NAME) -t
