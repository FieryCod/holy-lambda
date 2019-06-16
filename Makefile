SHELL := /bin/bash
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

.PHONY: use-nvm setup release

.DEFAULT_GOAL := setup

use-nvm:
	@. ~/.nvm/nvm.sh && nvm use > /dev/null 2>&1

setup: use-nvm
	@printf "\n ${GREEN} Installing Clojure dependencies ${NC}\n"
	@lein deps
	@printf "\n ${GREEN} Successfully installed all Clojure dependencies ${NC}\n"
	@printf "\n ${GREEN} Installing all node modules ${NC}\n"
	@npm install -g yarn
	@yarn
	@printf "${GREEN} Successfully installed all Node.js dependencies ${NC}\n"
	@printf "\n ${GREEN} Installing fnative... ${NC}\n"
	@bash -c "chmod +x resources/fnative && sudo cp resources/fnative /usr/local/bin/"
	@printf "\n ${GREEN} Sucessfully installed fnative utility... ${NC}\n"

install-fnative:
	@sudo cp -f resources/fnative /usr/local/bin

build-docker:
	@docker build . -f resources/Dockerfile -t fierycod/graalvm-native-image

push-docker:
	@docker push fierycod/graalvm-native-image

release: use-nvm
	@lein pom
	@lein deploy
	@./resources/github-tag

commit: use-nvm
	@yarn commit
