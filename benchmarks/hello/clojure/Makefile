build:
	@echo "Building Clojure artifacts"
	@clojure -X:uberjar :aot [hello] :jvm-opts '["-Dclojure.compiler.direct-linking=true", "-Dclojure.spec.skip-macros=true"]' :jar "../artifacts/clojure.jar" :main-class hello
