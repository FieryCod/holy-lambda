# Holy Lambda
[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda.svg?logo=clojure&logoColor=white)](https://clojars.org/io.github.FieryCod/holy-lambda)
[![CircleCI](https://circleci.com/gh/FieryCod/holy-lambda/tree/master.svg?style=svg)](https://circleci.com/gh/FieryCod/holy-lambda/tree/master)
[![codecov](https://codecov.io/gh/FieryCod/holy-lambda/branch/master/graph/badge.svg)](https://codecov.io/gh/FieryCod/holy-lambda)
[![cljdoc badge](https://cljdoc.org/badge/io.github.FieryCod/holy-lambda)](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT)
[![Slack](https://img.shields.io/badge/Slack-holy--lambda-blue?logo=slack)](https://clojurians.slack.com/messages/holy-lambda/)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

---

Tiny native AWS Custom Lambda Runtime which fulfills your needs!

``` clojure
[io.github.FieryCod/holy-lambda "0.1.29"]
```

## What it does?
It allows you to write one code which might run either on Official Java AWS Runtime or on Native Custom AWS Runtime built into your codebase.

[Jump here](https://cljdoc.org/d/fierycod/holy-lambda/CURRENT/doc/tutorial) to learn more and start the journey with the Holy Lambda.

## Example

``` clojure
(ns some.ns
  (:gen-class)
  (:require 
    [fierycod.holy-lambda.core :as h]
    [fierycod.holy-lambda.interceptor :as i]
    [fierycod.holy-lambda.native :as native]
    [fierycod.holy-lambda.response :as hr]))

(i/definterceptor LogIncomingRequest
  {:enter (fn [request] request)})
 
(h/deflambda ExampleLambda
  "I can run on both Java and Native..."
  < {:interceptors [LogIncomingRequest]}
  [{:keys [event ctx]}]
  (hr/text "Hello world"))
  
(native/entrypoint [#'ExampleLambda])
```

## Quickstart

Generate a new project from template via

``` 
lein new holy-lambda <your-project-name>
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
