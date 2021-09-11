``` clojure
io.github.FieryCod/holy-lambda                     {:mvn/version "0.5.0-SNAPSHOT"}

;; Default retriever is built in holy-lambda. For `core.async` support use `async-retriever`
io.github.FieryCod/holy-lambda-default-retriever   {:mvn/version "0.0.7"}

;; Not supported in babashka runtime
io.github.FieryCod/holy-lambda-async-retriever     {:mvn/version "0.0.7"}

;; Babashka tasks docker images
fierycod/graalvm-native-image:ce                   ;; GraalVM CE 21.2.0
fierycod/graalvm-native-image:dev                  ;; GraalVM CE-dev 21.3.0-dev_20210817_2030 (https://github.com/graalvm/graalvm-ce-dev-builds/releases/)

;; In bb.edn :deps
io.github.FieryCod/holy-lambda-babashka-tasks      {:git/url     "https://github.com/FieryCod/holy-lambda"
                                                    :deps/root   "./modules/holy-lambda-babashka-tasks"
                                                    :sha         "8b948be359f3556523a0b553050a20569af0224d"}
```

