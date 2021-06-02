USER_GID=$$(id -u):$$(id -g)
PWD=$$(pwd)
IMAGE_CORD_PART:=fierycod/graalvm-native-image
USE_EE=false

ifeq ($(USE_EE), false)
	IMAGE_CORD=$(IMAGE_CORD_PART):ce
else
	IMAGE_CORD=$(IMAGE_CORD_PART):ee
endif

output.jar:
	@clojure -X:uberjar :aot [hello] :jvm-opts '["-Dclojure.compiler.direct-linking=true", "-Dclojure.spec.skip-macros=true"]' :jar "output.jar" :main-class hello

output:
	@docker run --user $(USER_GID) -v ${PWD}:/project -it $(IMAGE_CORD) bash -c "cd /project && \
		native-image -jar output.jar \
		  -H:+AllowIncompleteClasspath \
		  --report-unsupported-elements-at-runtime \
      --no-fallback \
      --verbose \
      --enable-url-protocols=http,https \
      --no-server \
      --initialize-at-build-time"

build: output.jar output
	@echo "Building Clojure Native artifacts"
	@chmod +x output && cp resources/bootstrap-native bootstrap && chmod +x bootstrap && zip -r -j clojure-native bootstrap output && rm -Rf bootstrap && mv clojure-native.zip ../artifacts/clojure-native.zip
