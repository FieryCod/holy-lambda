SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'
DEFAULT_NATIVE_PARAMETERS="ParameterKey=Runtime,ParameterValue=provided ParameterKey=CodeUri,ParameterValue=."

.PHONY: install-holy-lambda native-dry-run compile dry-run native-compile

install-holy-lambda:
	@bash -c "cd ../../ && lein install"

compile:
	@lein uberjar

dry-api:
	@sam local start-api --skip-pull-image

native-compile:
	@fnative "-jar target/hello-lambda.jar --report-unsupported-elements-at-runtime --no-fallback --enable-url-protocols=http,https --no-server -J-Xmx3G -J-Xms3G"

native-dry-api:
	@rm -Rf bootstrap
	@mv hello-lambda bootstrap
	sam local start-api --parameter-overrides='$(DEFAULT_NATIVE_PARAMETERS)'
