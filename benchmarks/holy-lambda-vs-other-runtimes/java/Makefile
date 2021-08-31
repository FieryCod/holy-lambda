USER_GID=$$(id -u):$$(id -g)
PWD=$$(pwd)
IMAGE_CORD_PART:=fierycod/graalvm-native-image
USE_EE=false

ifeq ($(USE_CE11), true)
	IMAGE_CORD=$(IMAGE_CORD_PART):ce-11
else ifeq ($(USE_CE), true)
	IMAGE_CORD=$(IMAGE_CORD_PART):ce
else
	IMAGE_CORD=$(IMAGE_CORD_PART):ee
endif

output8.jar output11.jar:
	bash -c "source $(SDKMAN_DIR)/bin/sdkman-init.sh && \
		sdk use java 8.292.10.1-amzn && \
		mvn versions:set-property -Dproperty=maven.compiler.target -DnewVersion=1.8 && \
		mvn clean install && \
    cp target/java-1.8.jar output8.jar && \
		mv target/java-1.8.jar ../artifacts/java-8.jar && \
		sdk use java 11.0.11.9.1-amzn && \
		mvn versions:set-property -Dproperty=maven.compiler.target -DnewVersion=11 && \
		mvn clean install && \
    cp target/java-11.jar output11.jar && \
		mv target/java-11.jar ../artifacts/java-11.jar"

output8: output8.jar
	@docker run --user $(USER_GID) -v ${PWD}:/project -it fierycod/graalvm-native-image:ce bash -c "cd /project && \
		native-image -jar output8.jar \
		  -H:+AllowIncompleteClasspath \
		  --report-unsupported-elements-at-runtime \
			-H:ConfigurationFileDirectories=resources \
			--no-fallback \
      --verbose \
      --enable-url-protocols=http,https \
			--no-server \
      --initialize-at-build-time=org.slf4j"

output11: output11.jar
	@docker run --user $(USER_GID) -v ${PWD}:/project -it fierycod/graalvm-native-image:ce-11 bash -c "cd /project && \
		native-image -jar output11.jar \
		  -H:+AllowIncompleteClasspath \
		  --report-unsupported-elements-at-runtime \
			-H:ConfigurationFileDirectories=resources \
			--no-fallback \
      --verbose \
      --enable-url-protocols=http,https \
			--no-server \
      --initialize-at-build-time=org.slf4j"

clean:
	rm -Rf output8.jar output8 output11.jar output11 target

build8: output8.jar output8
	@chmod +x output8 && cp resources/bootstrap-native-8 bootstrap && chmod +x bootstrap && zip -r -j java8 bootstrap output8 && rm -Rf bootstrap && mv java8.zip ../artifacts/

build11: output11.jar output11
	@chmod +x output11 && cp resources/bootstrap-native-11 bootstrap && chmod +x bootstrap && zip -r -j java11 bootstrap output11 && rm -Rf bootstrap && mv java11.zip ../artifacts/

build: output8.jar output11.jar build8 build11
	@echo "Building Java Native artifacts"
