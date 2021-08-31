all:
	@mkdir -p build
	@rm -rf ./build/*
	@stack clean --docker
	@stack build --docker
	@cp `stack --docker path --local-install-root`/bin/bootstrap build
	@cd build && zip haskell.zip  bootstrap && rm bootstrap && cd ..
	@mv build/haskell.zip ../artifacts/
