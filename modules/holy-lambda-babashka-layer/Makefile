BUCKET_NAME=holy-lambda-babashka-layer
S3_PREFIX=holy-lambda
SEMANTIC_VERSION=`cat LAYER_VERSION`
APP_REGION=eu-central-1

all:
	@echo "Building the artifacts"

	@docker build --target BUILDER -t holy-lambda-babashka-lambda-archiver .
	@docker rm build || true
	@docker create --name build holy-lambda-babashka-lambda-archiver
	@docker cp build:/opt/holy-lambda-babashka-runtime.zip holy-lambda-babashka-runtime.zip
	@docker rm build || true

	@echo "Publishing a new version of the layer"
	@sam package --template-file template.yml --output-template-file packaged.yml --s3-bucket $(BUCKET_NAME) --s3-prefix $(S3_PREFIX)
	@sam publish --template-file packaged.yml --semantic-version $(SEMANTIC_VERSION)

destroy-bucket:
	@aws s3 ls s3://$(BUCKET_NAME) && aws s3 rb s3://$(BUCKET_NAME) --force --region $(APP_REGION)

make-bucket:
	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
