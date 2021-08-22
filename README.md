# Holy Lambda
[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda.svg?logo=clojure&logoColor=white)](https://clojars.org/io.github.FieryCod/holy-lambda)
[![CircleCI](https://circleci.com/gh/FieryCod/holy-lambda/tree/master.svg?style=svg)](https://circleci.com/gh/FieryCod/holy-lambda/tree/master)
[![codecov](https://codecov.io/gh/FieryCod/holy-lambda/branch/master/graph/badge.svg)](https://codecov.io/gh/FieryCod/holy-lambda)
[![Slack](https://img.shields.io/badge/Slack-holy--lambda-blue?logo=slack)](https://clojurians.slack.com/messages/holy-lambda/)
[![DockerHub](https://img.shields.io/docker/pulls/fierycod/graalvm-native-image.svg?logo=docker)](https://hub.docker.com/r/fierycod/graalvm-native-image)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
[![Build Status](https://dev.azure.com/vetcard/holy-lambda/_apis/build/status/FieryCod.holy-lambda?branchName=master)](https://dev.azure.com/vetcard/holy-lambda/_build/latest?definitionId=2&branchName=master)

---

The extraordinary simple, performant, and extensible micro framework that integrates Clojure with AWS Lambda on either Java, Clojure Native, or Babashka runtime. 

**Supported runtimes**
  - Babashka
  - Native Clojure runtime
  - Clojure custom runtime that is faster for Clojure, than the offical Java runtime
  - (incoming) Node.js runtime for Clojurescript

**Stable releases**

``` clojure
io.github.FieryCod/holy-lambda                     {:mvn/version "0.2.3"}

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

[Jump here](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT/doc/tutorial) to learn more and start the journey with the Holy Lambda.

### Quick start
#### Minimal Code
``` clojure
(ns some.ns
  (:gen-class)
  (:require 
    [fierycod.holy-lambda.core :as h]
    [fierycod.holy-lambda.response :as hr]))
    
(defn ExampleLambda
  "I can run on Java, Babashka or Native runtime..."
  [{:keys [event ctx]}]
  (hr/text "Hello world"))
  
(h/entrypoint [#'ExampleLambda])
```
#### Project scaffolding

``` clojure
clojure -M:new -m clj-new.create holy-lambda basic.example && cd basic.example && bb stack:sync
```

Alternatively you can use `lein new`:

``` clojure
lein new holy-lambda example ;; <-- Replace `example` with the name of the project
```

#### Available helpers

``` sh

❯ bb tasks
The following tasks are available:

docker:run             > Run command in fierycod/graalvm-native-image docker context 

----------------------------------------------------------------

native:conf            > Provides native configurations for the application
native:executable      > Provides native executable of the application

----------------------------------------------------------------

stack:sync             > Syncs project & dependencies from either:
                        - <Clojure>  deps.edn
                        - <Babashka> bb.edn:runtime:pods
stack:compile          > Compiles sources if necessary
                        - :force         - force compilation even if sources did not change
stack:doctor           > Diagnoses common issues of holy-lambda stack
stack:purge            > Purges build artifacts
stack:version          > Outputs holy-lambda babashka tasks version
```

## Thanks to
- Daria - Thank you that you were always beside me, fighting for me when I had no faith and energy.
- @KrzysztofTucholski - Thank you for boosting my ego
- @uswitch - Thank you for interests in AWS Lambda Functions and providing us the `lambada`
- @hjhamala - Thank you for sharing [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html) about native lambda functions. You have inspired me to write `holy-lambda`.
- Rum - deflambda parse mechanism is adapted from rum.
- Ring - code from fierycod.holy-lambda.response is adapted from ring-core. 

## License
Copyright © 2021 Karol Wojcik aka Fierycod

Released under the MIT license.
