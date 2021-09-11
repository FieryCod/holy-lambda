USE_EE:=false
IMAGE_CORD_PART:=fierycod/graalvm-native-image
IMAGE_BUILD:=ce
IMAGE_BUILD_POSTFIX:=""
VERSION_BUMP=patch
USE_DEV:=false

ifeq ($(USE_DEV), true)
	IMAGE_CORD=$(IMAGE_CORD_PART):dev
else ifeq ($(USE_EE), false)
	IMAGE_CORD=$(IMAGE_CORD_PART):ce-11
else ifeq ($(USE_M1), false)
	IMAGE_CORD=$(IMAGE_CORD_PART):m1
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


ifeq ($(USE_M1), true)
	IMAGE_BUILD_POSTFIX=".m1"
else
	IMAGE_BUILD_POSTFIX=""
endif

build-docker:
	@docker build docker -f docker/Dockerfile.$(IMAGE_BUILD)$(IMAGE_BUILD_POSTFIX) -t $(IMAGE_CORD)

push-docker:
ifeq ($(USE_EE), true)
	@echo "GraalVM EE is only for development & internal benchmarks. Do not publish it!"
else
	docker push $(IMAGE_CORD)
endif

test:
	@lein with-profile eftest eftest

cache-m2:
	bash -c "cd ~/.m2/repository/ && zip -r m2-cached.zip . && rm -Rf ~/azure-agent/local_cache/m2-cached.zip && mv m2-cached.zip ~/azure-agent/local_cache/"
