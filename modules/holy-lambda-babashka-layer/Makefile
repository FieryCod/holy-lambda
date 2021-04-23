BUCKET_NAME=holy-lambda-babashka-layer
S3_PREFIX=holy-lambda

all:
	@echo "Building the artifacts"

	docker build --target BUILDER -t holy-lambda-babashka-lambda-archiver .
	docker rm build || true
	docker create --name build holy-lambda-babashka-lambda-archiver
	docker cp build:/var/task/holy-lambda-babashka-runtime.zip holy-lambda-babashka-runtime.zip

	@echo "Publishing a new version of the layer"
	sam package --template-file template.yml --output-template-file packaged.yml --s3-bucket $(BUCKET_NAME) --s3-prefix $(S3_PREFIX)
	sam publish
