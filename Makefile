install-graalvm:
	@wget -O graalvm.tar.gz https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java8-linux-amd64-20.3.0.tar.gz
	@mkdir -p .graalvm && tar xvzf graalvm.tar.gz -C .graalvm --strip-components 1
	@mv .graalvm/ ~/.graalvm
	@echo "Use \"export GRAALVM=~/.graalvm/bin/\" in either .zshrc or .bashrc"
	@rm -Rf graalvm.tar.gz

build-docker:
	@docker build . -f resources/Dockerfile -t fierycod/graalvm-native-image

push-docker:
	@docker push fierycod/graalvm-native-image
