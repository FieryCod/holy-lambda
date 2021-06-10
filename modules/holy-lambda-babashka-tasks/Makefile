clean:
	rm -Rf holy-lambda-babashka-runtime

local-layer: clean
	rm -Rf holy-lambda-babashka-runtime
	bash -c "cd ../holy-lambda-babashka-layer && make build-artifact-in-place"
	mv ../holy-lambda-babashka-layer/holy-lambda-babashka-runtime.zip .
	mkdir -p holy-lambda-babashka-runtime
	unzip holy-lambda-babashka-runtime.zip -d holy-lambda-babashka-runtime
	rm -Rf holy-lambda-babashka-runtime.zip
