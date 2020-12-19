# GraalVM releases:
# https://github.com/graalvm/graalvm-ce-builds/releases

UNAME_S := $(shell uname -s)
ifeq ($(UNAME_S),Linux)
	GRAALVM_TAR_GZ=https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java8-linux-amd64-20.3.0.tar.gz
        STRIP_COMPONENTS=1
        GRAALVM_BIN=~/.graalvm/bin
else
        ifeq ($(UNAME_S),Darwin)
           GRAALVM_TAR_GZ=https://github.com/graalvm/graalvm-ce-builds/releases/download/vm-20.3.0/graalvm-ce-java11-darwin-amd64-20.3.0.tar.gz
           STRIP_COMPONENTS=1
           GRAALVM_BIN=~/.graalvm/Contents/Home/bin
           NATIVE_INSTALL="${GRAALVM_BIN}/gu install native-image"
        endif
endif

.PHONY: install-graalvm

graalvm.tar.gz:
	@wget -O $@ ${GRAALVM_TAR_GZ}

install-graalvm: graalvm.tar.gz
	@mkdir -p .graalvm && tar xvzf graalvm.tar.gz -C .graalvm --strip-components ${STRIP_COMPONENTS}
	@mv .graalvm/ ~/.graalvm
	if [ ${NATIVE_INSTALL} != "" ]; then bash -c ${NATIVE_INSTALL}; fi
	@echo "Use \"export GRAALVM=~/.graalvm/bin/\" in either .zshrc or .bashrc"
	@rm -Rf graalvm.tar.gz

build-docker:
	@docker build . -f resources/Dockerfile -t fierycod/graalvm-native-image

push-docker:
	@docker push fierycod/graalvm-native-image
