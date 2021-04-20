USE_EE:=false
IMAGE_CORD_PART:=fierycod/graalvm-native-image
IMAGE_BUILD:=ce
VERSION_BUMP=patch

ifeq ($(USE_EE), false)
	IMAGE_CORD=$(IMAGE_CORD_PART)-ce:latest
else
	IMAGE_CORD=$(IMAGE_CORD_PART)-ee:latest
endif

ifeq ($(USE_EE), false)
	IMAGE_BUILD=ce
else
	IMAGE_BUILD=ee
endif

build-docker:
	@docker build docker/$(IMAGE_BUILD) -f docker/$(IMAGE_BUILD)/Dockerfile -t $(IMAGE_CORD)

push-docker:
ifeq ($(USE_EE), true)
	@echo "GraalVM EE is only for development & internal benchmarks. Do not publish it!"
else
	docker push $(IMAGE_CORD)
endif

deploy:
	@clojure deployer.clj $(VERSION_BUMP)
