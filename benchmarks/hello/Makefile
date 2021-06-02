.PHONY: build

build: artifacts/clojure-native.zip artifacts/clojure.jar artifacts/golang artifacts/latest-rust.zip
	@echo "All deployable artifacts has been build!"

artifacts/clojure.jar:
	@cd clojure && make build

artifacts/golang:
	@cd golang && make build

artifacts/clojure-native.zip:
	@cd clojure-native && make build

artifacts/latest-rust.zip:
	@cd rust && make build

clean:
	rm -Rf artifacts/latest-rust.zip artifacts/clojure-native.zip artifacts/clojure-native.zip artifacts/golang artifacts/clojure.jar
