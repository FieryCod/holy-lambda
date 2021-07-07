USE_EE:=false
IMAGE_CORD_PART:=fierycod/graalvm-native-image
IMAGE_BUILD:=ce
VERSION_BUMP=patch
USE_DEV:=false

ifeq ($(USE_DEV), true)
	IMAGE_CORD=$(IMAGE_CORD_PART):dev
else ifeq ($(USE_EE), false)
	IMAGE_CORD=$(IMAGE_CORD_PART):ce
else
	IMAGE_CORD=$(IMAGE_CORD_PART):ee
endif

ifeq ($(USE_DEV), true)
	IMAGE_BUILD=dev
else ifeq ($(USE_EE), false)
	IMAGE_BUILD=ce
else
	IMAGE_BUILD=ee
endif

build-docker:
	echo $(USE_DEV)
	@docker build docker -f docker/Dockerfile.$(IMAGE_BUILD) -t $(IMAGE_CORD)

push-docker:
ifeq ($(USE_EE), true)
	@echo "GraalVM EE is only for development & internal benchmarks. Do not publish it!"
else
	docker push $(IMAGE_CORD)
endif

verify-cljdoc:
	@curl -fsSL https://raw.githubusercontent.com/cljdoc/cljdoc/master/script/verify-cljdoc-edn | bash -s doc/cljdoc.edn

test:
	@lein with-profile eftest eftest
