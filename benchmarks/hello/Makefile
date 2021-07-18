.PHONY: build
BUCKET_NAME:=none

build: artifacts/clojure8.zip artifacts/clojure11.zip artifacts/clojure8.jar artifacts/clojure11.jar artifacts/golang-handler artifacts/latest-rust.zip artifacts/haskell.zip artifacts/csharp.zip artifacts/java-8.jar artifacts/java-11.jar artifacts/java8.zip artifacts/java11.zip
	@echo "All deployable artifacts has been build!"

artifacts/clojure8.jar artifacts/clojure11.jar:
	@cd clojure && make build

artifacts/golang:
	@cd golang && make build

artifacts/clojure8.zip artifacts/clojure11.zip:
	@cd clojure-native && make build

artifacts/latest-rust.zip:
	@cd rust && make build

artifacts/haskell.zip:
	@cd haskell && make

artifacts/csharp.zip:
	@cd csharp && make

artifacts/java8.zip artifacts/java11.zip artifacts/java-8.jar artifacts/java-11.jar:
	@cd java && make build

clean:
	rm -Rf artifacts/clojure8.zip artifacts/clojure11.zip artifacts/clojure8.jar artifacts/clojure11.jar artifacts/golang-handler artifacts/latest-rust.zip artifacts/haskell.zip artifacts/csharp.zip artifacts/java-8.jar artifacts/java-11.jar artifacts/java8.zip artifacts/java11.zip

deploy-128:
	BUCKET_NAME=hlbench-hello-128

	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
	@sam package --template-file template128.yml --output-template-file packaged-128.yml --s3-bucket $(BUCKET_NAME) --s3-prefix $(S3_PREFIX)

test:
	@echo "---------------------------------------------------------"
	@echo "Java runtimes check"
	@echo "---------------------------------------------------------"
	@echo "Java8 Runtime: $$(sam local invoke -t template128.yml Java8Runtime 2>/dev/null)"
	@echo "Java11 Runtime: $$(sam local invoke -t template128.yml Java11Runtime 2>/dev/null)"
	@echo "---------------------------------------------------------"
	@echo ""
	@echo "---------------------------------------------------------"
	@echo "Java native runtimes check"
	@echo "---------------------------------------------------------"
	@echo "Java8 Runtime: $$(sam local invoke -t template128.yml JavaGraalVM211CE8 2>/dev/null)"
	@echo "Java11 Runtime: $$(sam local invoke -t template128.yml Java11Runtime 2>/dev/null)"
	@echo "---------------------------------------------------------"
