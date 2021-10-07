test:
	@lein with-profile eftest eftest

cache-m2:
	bash -c "mkdir -p ~/.az-agent/local_cache && cd ~/.m2/repository/ && zip -r m2-cached.zip . && rm -Rf ~/.az-agent/local_cache/m2-cached.zip && mv m2-cached.zip ~/.az-agent/local_cache/"
