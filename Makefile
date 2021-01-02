build-docker:
	@docker build . -f resources/Dockerfile -t fierycod/graalvm-native-image

push-docker:
	@docker push fierycod/graalvm-native-image
