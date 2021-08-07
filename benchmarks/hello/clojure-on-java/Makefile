build:
	@echo "Building Clojure artifacts"
	bash -c "source $(SDKMAN_DIR)/bin/sdkman-init.sh && \
		sdk use java 8.292.10.1-amzn && \
		clojure -X:uberjar :aot [hello] :jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar \"../artifacts/clojure-on-java-8.jar\" :main-class hello && \
		sdk use java 11.0.11.9.1-amzn && \
		clojure -X:uberjar :aot [hello] :jvm-opts '[\"-Dclojure.compiler.direct-linking=true\", \"-Dclojure.spec.skip-macros=true\"]' :jar \"../artifacts/clojure-on-java-11.jar\" :main-class hello"
