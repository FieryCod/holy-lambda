.PHONY: build
BUCKET_NAME:=none
STACK_NAME:=hl-hello-benches-v1

build: artifacts/clojure8.zip artifacts/clojure11.zip artifacts/clojure-on-java-8.jar artifacts/clojure-on-java-11.jar artifacts/golang-handler artifacts/latest-rust.zip artifacts/haskell.zip artifacts/csharp.zip artifacts/java-8.jar artifacts/java-11.jar artifacts/java8.zip artifacts/java11.zip
	@cd clojure-on-clojure && make build
	@echo "All deployable artifacts has been build!"

artifacts/clojure-on-java-8.jar artifacts/clojure-on-java-11.jar:
	@cd clojure-on-java && make build

artifacts/golang-handler:
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
ifeq ($(BUCKET_NAME), none)
	make BUCKET_NAME=hl-hellobench-128v1 deploy-128
else
	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
	@sam package --template-file template128.yml --output-template-file packaged-128.yml --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
	@sam deploy  --template-file packaged-128.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region eu-central-1 --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
endif

deploy-512:
ifeq ($(BUCKET_NAME), none)
	make STACK_NAME=hl-hello-benches-512-v2 BUCKET_NAME=hl-hellobench-512v1 deploy-512
else
	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
	@sam package --template-file template512.yml --output-template-file packaged-512.yml --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
	@sam deploy  --template-file packaged-512.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region eu-central-1 --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
endif

deploy-1024:
ifeq ($(BUCKET_NAME), none)
	make STACK_NAME=hl-hello-benches-1024-v1 BUCKET_NAME=hl-hellobench-1024v1 deploy-1024
else
	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
	@sam package --template-file template1024.yml --output-template-file packaged-1024.yml --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
	@sam deploy  --template-file packaged-1024.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region eu-central-1 --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
endif

deploy-2048:
ifeq ($(BUCKET_NAME), none)
	make STACK_NAME=hl-hello-benches-2048-v1 BUCKET_NAME=hl-hellobench-2048v1 deploy-2048
else
	@aws s3 ls s3://$(BUCKET_NAME) || aws s3 mb s3://$(BUCKET_NAME)
	@sam package --template-file template2048.yml --output-template-file packaged-2048.yml --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
	@sam deploy  --template-file packaged-2048.yml --stack-name $(STACK_NAME) --capabilities CAPABILITY_IAM --region eu-central-1 --s3-bucket $(BUCKET_NAME) --image-repository 443526418261.dkr.ecr.eu-central-1.amazonaws.com/fierycod/clojure-on-clojure
endif

destroy-128:
ifeq ($(BUCKET_NAME), none)
	make BUCKET_NAME=hl-hellobench-128v1 destroy-128
else
	@aws cloudformation delete-stack --stack-name $(STACK_NAME) --region eu-central-1
endif

destroy-512:
ifeq ($(BUCKET_NAME), none)
	make BUCKET_NAME=hl-hellobench-512v1 destroy-512
else
	STACK_NAME=hl-hello-benches-512-v2 aws cloudformation delete-stack --stack-name $(STACK_NAME) --region eu-central-1
endif

destroy-1024:
ifeq ($(BUCKET_NAME), none)
	make BUCKET_NAME=hl-hellobench-1024v1 destroy-1024
else
	STACK_NAME=hl-hello-benches-1024-v1 aws cloudformation delete-stack --stack-name $(STACK_NAME) --region eu-central-1
endif

destroy-2048:
ifeq ($(BUCKET_NAME), none)
	make BUCKET_NAME=hl-hellobench-2048v1 destroy-2048
else
	STACK_NAME=hl-hello-benches-2048-v1 aws cloudformation delete-stack --stack-name $(STACK_NAME) --region eu-central-1
endif

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

benchmark-128:
	COLD=1 bb test.clj 128

benchmark-512:
	COLD=1 bb test.clj 512

benchmark-1024:
	COLD=1 bb test.clj 1024

benchmark-2048:
	COLD=1 bb test.clj 2048
