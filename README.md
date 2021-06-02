# Holy Lambda
[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda.svg?logo=clojure&logoColor=white)](https://clojars.org/io.github.FieryCod/holy-lambda)
[![CircleCI](https://circleci.com/gh/FieryCod/holy-lambda/tree/master.svg?style=svg)](https://circleci.com/gh/FieryCod/holy-lambda/tree/master)
[![codecov](https://codecov.io/gh/FieryCod/holy-lambda/branch/master/graph/badge.svg)](https://codecov.io/gh/FieryCod/holy-lambda)
[![cljdoc badge](https://cljdoc.org/badge/io.github.FieryCod/holy-lambda)](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT)
[![Slack](https://img.shields.io/badge/Slack-holy--lambda-blue?logo=slack)](https://clojurians.slack.com/messages/holy-lambda/)
[![DockerHub](https://img.shields.io/docker/pulls/fierycod/graalvm-native-image.svg?logo=docker)](https://hub.docker.com/r/fierycod/graalvm-native-image)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

---

The micro framework that integrates Clojure with AWS Lambda on either Java, Clojure Native, or Babashka runtime. 

**Supported runtimes**
  - Babashka
  - Native Clojure runtime
  - Java based Clojure runtime
  - (incoming) Node.js runtime for Clojurescript

**Stable releases**

``` clojure
;; Babashka runtime layer `0.4.4`
;; `:runtime:version` are the inner properties of `:holy-lambda/options` in bb.edn
:holy-lambda/options:runtime:version               "0.0.32" 

io.github.FieryCod/holy-lambda                     {:mvn/version "0.1.49"}

;; Built in holy-lambda. For core.async support use `async-retriever`
io.github.FieryCod/holy-lambda-default-retriever   {:mvn/version "0.0.6"}

;; Not supported in babashka runtime
io.github.FieryCod/holy-lambda-async-retriever     {:mvn/version "0.0.6"}

;; In bb.edn :deps
io.github.FieryCod/holy-lambda-babashka-tasks      {:git/url "https://github.com/FieryCod/holy-lambda"
                                                    :deps/root "./modules/holy-lambda-babashka-tasks"
                                                    :sha     "f2ff90b23120482e1e8861e083de6ff7aebc8a99"}
```

[Jump here](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT/doc/tutorial) to learn more and start the journey with the Holy Lambda.

**Interceptors namespace is work in progress and subject to change. Consider it as an alpha.**

### Minimal code example

``` clojure
(ns some.ns
  (:gen-class)
  (:require 
    [fierycod.holy-lambda.core :as h]
    [fierycod.holy-lambda.interceptor :as i]
    [fierycod.holy-lambda.native :as native]
    [fierycod.holy-lambda.response :as hr]))

(i/definterceptor LambdaLogger
  {:enter (fn [request]
            (println "REQUEST:" request)
            request)
   :leave (fn [response]
            (println "RESPONSE:" response)
            response)})
 
(h/deflambda ExampleLambda
  "I can run on Java, Babashka or Native runtime..."
  < {:interceptors [LambdaLogger]}
  [{:keys [event ctx]}]
  (hr/text "Hello world"))
  
(native/entrypoint [#'ExampleLambda])
```

## Quickstart

Generate a new project from template via:

``` clojure
clojure -M:new -m clj-new.create holy-lambda basic.example && cd basic.example && bb stack:sync
```

Alternatively you can use `lein new`:

``` clojure
lein new holy-lambda example ;; <-- Replace `example` with the name of the project
```

## What it does?
It allows you to write one code which might run on Official Java AWS Runtime, Native Custom AWS Runtime built into your codebase or Babashka runtime. Holy Lambda ships with `Babashka tasks` to ease development and deployment of an application. 

Available tasks:

``` sh
❯ bb tasks
The following tasks are available:

bucket:create          > Creates a s3 bucket using :bucket-name
bucket:remove          > Removes a s3 bucket using :bucket-name

----------------------------------------------------------------

docker:build:ee        > Builds local image for GraalVM EE 
docker:run             > Run command in fierycod/graalvm-native-image docker context

----------------------------------------------------------------

native:conf            > Provides native configurations for the application
                        - :runtime     - overrides :runtime:name and run Lambda in specified runtime 
native:executable      > Provides native executable of the application
                        - :runtime     - overrides :runtime:name and run Lambda in specified runtime

----------------------------------------------------------------

stack:sync             > Syncs project & dependencies from either:
                        - <Clojure>  project.clj
                        - <Clojure>  deps.edn
                        - <Babashka> bb.edn:runtime:pods
stack:compile          > Compiles sources if necessary
stack:invoke           > Invokes lambda fn (check sam local invoke --help):
                        - :name        - either :name or :stack:default-lambda
                        - :event-file  - path to event file
                        - :envs-file   - path to envs file
                        - :params      - map of parameters to override in AWS SAM
                        - :runtime     - overrides :runtime:name and run Lambda in specified runtime
                        - :debug       - run invoke in debug mode
                        - :logs        - logfile to runtime logs to
stack:api              > Runs local api (check sam local start-api):
                        - :debug       - run api in debug mode
                        - :port        - local port number to listen to
                        - :static-dir  - assets which should be presented at /
                        - :envs-file   - path to envs file
                        - :runtime     - overrides :runtime:name and run Lambda in specified runtime
                        - :params      - map of parameters to override in AWS SAM
stack:pack             > Packs Cloudformation stack
                        - :runtime     - overrides :runtime:name and run Lambda in specified runtime 
stack:deploy           > Deploys Cloudformation stack
                        - :guided      - guide the deployment
                        - :dry         - execute changeset?
                        - :params      - map of parameters to override in AWS SAM
                        - :runtime     - overrides :runtime:name and run Lambda in specified runtime 
stack:describe         > Describes Cloudformation stack
stack:doctor           > Diagnoses common issues of holy-lambda stack
stack:purge            > Purges build artifacts
stack:destroy          > Destroys Cloudformation stack & removes bucket
stack:logs             > Possible arguments (check sam logs --help):
                        - :name        - either :name or :stack:default-lambda
                        - :e           - fetch logs up to this time
                        - :s           - fetch logs starting at this time
                        - :filter      - find logs that match terms 
stack:version          > Outputs holy-lambda babashka tasks version
stack:lint             > Lints the project
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
