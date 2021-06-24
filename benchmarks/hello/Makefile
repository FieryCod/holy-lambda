.PHONY: build
BUCKET_NAME:=none

build: artifacts/clojure-native.zip artifacts/clojure.jar artifacts/golang artifacts/latest-rust.zip
	@echo "All deployable artifacts has been build!"

artifacts/clojure.jar:
	@cd clojure && make build

artifacts/golang:
	@cd golang && make build

artifacts/clojure-native.zip:
	@cd clojure-native && make build

artifacts/latest-rust.zip:
	@cd rust && make build

clean:
	rm -Rf artifacts/latest-rust.zip artifacts/clojure-native.zip artifacts/clojure-native.zip artifacts/golang artifacts/clojure.jar

deploy-128:
	BUCKET_NAME=bench-hello-128

	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
	@sam package --template-file template128.yml --output-template-file packaged-128.yml --s3-bucket $(BUCKET_NAME) --s3-prefix $(S3_PREFIX)
