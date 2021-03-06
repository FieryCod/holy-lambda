SHELL := /bin/bash

.PHONY: all check-service clean deploy deploy-native destroy-stack destroy-bucket dry-api dry-api-native local-test make-bucket

BUCKET_NAME=holy-lambda-babashka-example-$$(whoami)
STACK_NAME=hello-babashka
S3_PREFIX=hello-babashka
APP_REGION=eu-central-1
PWD=$$(pwd)
USER_GID=$$(id -u):$$(id -g)
LAMBDA_NAME=HelloBabashkaFunction
USE_EE:=false
IMAGE_CORD_PART:=fierycod/graalvm-native-image
USE_BB=false

ifeq ($(USE_EE), false)
	IMAGE_CORD=$(IMAGE_CORD_PART):ce
else
	IMAGE_CORD=$(IMAGE_CORD_PART):ee
endif

all: deploy-native

check-service:
	@(nc -z localhost 3000) || echo "AWS API service is not running: run 'make dry-api' or 'make dry-api-native."

clean:
	@rm -rf server target/ packaged.yml packaged-native.yml output latest.zip

deploy: packaged.yml
	sam deploy --template-file packaged.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

deploy-native: packaged-native.yml
	sam deploy --template-file packaged-native.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region $(APP_REGION)

destroy-stack:
	aws cloudformation delete-stack --stack-name $(STACK_NAME) --region $(APP_REGION)

dry-api: target/output.jar
	sam local start-api --template template.yml

native-gen-conf:
ifeq ($(USE_BB), false)
	@USE_AGENT_CONTEXT=true lein uberjar
	@docker run --user $(USER_GID) \
		-e "AWS_CREDENTIAL_PROFILES_FILE=/project/.aws/credentials" \
		-e "AWS_CONFIG_FILE=/project/.aws/config" \
		-v $(PWD):/project \
		-v ~/.aws:/project/.aws:ro \
		-it $(IMAGE_CORD) \
		bash -c "cd /project && \
	          java -agentlib:native-image-agent=config-output-dir=resources/native-configuration \
			           -Dexecutor=native-agent \
			           -jar target/output.jar"
else
	@echo "Using babashka. Skipping generation of native configuration!"
endif

dry-api-native: latest.zip
	sam local start-api --template template-native.yml --debug

local-test: check-service
	curl "http://127.0.0.1:3000/"
	curl "http://127.0.0.1:3000/bye"

destroy-bucket:
	@aws s3 ls s3://$(BUCKET_NAME) && aws s3 rb s3://$(BUCKET_NAME) --force --region $(APP_REGION)

make-bucket:
	aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)

latest.zip: output
ifeq ($(USE_BB), false)
	chmod +x output && cp resources/bootstrap-native bootstrap && chmod +x bootstrap && cp -r resources/native-deps . && zip -r -j latest bootstrap output native-deps/* && rm -Rf bootstrap native-deps
else
	cp resources/bootstrap-native-babashka bootstrap && chmod +x bootstrap && cp target/output.jar . && cp -r resources/native-deps . && zip -r -j latest bootstrap output.jar native-deps/* && rm -Rf bootstrap native-deps output.jar
endif

output: target/output.jar native-gen-conf
ifeq ($(USE_BB), false)
	docker run --user $(USER_GID) -v ${PWD}:/project -it $(IMAGE_CORD) bash -c "cd /project && \
		native-image -jar target/output.jar \
		  -H:ConfigurationFileDirectories=resources/native-configuration \
		  -H:+AllowIncompleteClasspath \
		  --report-unsupported-elements-at-runtime \
      --no-fallback \
      --verbose \
      --enable-url-protocols=http,https \
      --no-server \
      --initialize-at-build-time \
      --trace-object-instantiation=java.lang.Thread"
else
	@echo "Using babashka. Skipping native image compile"
endif

packaged.yml: target/output.jar make-bucket
	sam package --template-file template.yml --output-template-file packaged.yml --s3-bucket $(BUCKET_NAME) --s3-prefix $(S3_PREFIX)

packaged-native.yml: latest.zip make-bucket
	sam package --template-file template-native.yml --output-template-file packaged-native.yml --s3-bucket $(BUCKET_NAME) --s3-prefix $(S3_PREFIX)

target/output.jar: src/babashka_example/*.cljc
	@lein uberjar

logs-tail:
	@sam logs -n $(LAMBDA_NAME) --stack-name $(STACK_NAME) -t
