create-repository:
	aws ecr create-repository --repository-name fierycod/clojure-on-clojure-tiered || echo "Already created!"
	aws ecr describe-repositories | jq '.repositories[2].repositoryUri'

build:
	@rm -Rf clojure-on-clojure-tiered-8.jar clojure-on-clojure-tiered-11.jar
	@echo "Building Clojure artifacts"
	bash -c "source $(SDKMAN_DIR)/bin/sdkman-init.sh && \
		sdk use java 8.292.10.1-amzn && \
		clojure -X:uberjar :aot [hello] :jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar \"clojure-on-clojure-tiered-8.jar\" :main-class hello && \
		sdk use java 11.0.11.9.1-amzn && \
		clojure -X:uberjar :aot [hello] :jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar \"clojure-on-clojure-tiered-11.jar\" :main-class hello"
	docker build . -f Dockerfile.java11 -t fierycod/clojure-on-clojure-tiered:11
	docker build . -f Dockerfile.java8 -t fierycod/clojure-on-clojure-tiered:8
	docker tag fierycod/clojure-on-clojure-tiered:8 $$(aws ecr describe-repositories | jq -r '.repositories[2].repositoryUri'):8
	docker tag fierycod/clojure-on-clojure-tiered:11 $$(aws ecr describe-repositories | jq -r '.repositories[2].repositoryUri'):11
	docker push $$(aws ecr describe-repositories | jq -r '.repositories[2].repositoryUri'):11
	docker push $$(aws ecr describe-repositories | jq -r '.repositories[2].repositoryUri'):8
