PWD=$$(pwd)
IMAGE_EXISTS=$$(docker images -q amazon-golang:latest)

build: build-docker
	@docker create --name build amazon-golang
	@docker cp build:/handler handler
	@docker rm build || true
	@mv handler ../artifacts/golang-handler

build-docker:
	[ -n "$(IMAGE_EXISTS)" ] || docker build . -t amazon-golang
