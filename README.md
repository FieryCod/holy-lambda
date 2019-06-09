# Holy Lambda

[![Clojars Project](https://img.shields.io/clojars/v/fierycod/holy-lambda.svg?logo=clojure&logoColor=white)](https://clojars.org/fierycod/holy-lambda)
[![Downloads](https://jarkeeper.com/fierycod/holy-lambda/downloads.svg)](https://jarkeeper.com/fierycod/holy-lambda)
[![Dependencies Status](https://jarkeeper.com/fierycod/holy-lambda/status.svg)](https://jarkeeper.com/fierycod/holy-lambda)
[![Open Source Love](https://badges.frapsoft.com/os/v1/open-source.svg?v=103)](https://github.com/ellerbrock/open-source-badges/)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

---

Tiny native AWS Custom Lambda Runtime which fulfills your needs!

## What it does?
It allows you to write one code which might run either on Official Java AWS Runtime (if you don't care about speed or you want to test your lambdas)
or on Native Custom AWS Runtime built into your codebase.

## Prerequisites
You will need following things to start:
- Node.js >= v8.9.0
- Java >= 8
- Docker, Docker Compose >= 1.13.1, 1.22.0
- Python, Pip >= 3.5.0, 8.9.0

The last step is to install `fnative` which is a tiny wrapper which builds the `.jar` using `GraalVM` and outputs the binary in your project
1. Install `fnative` in your path.
   ```
   wget https://raw.githubusercontent.com/FieryCod/holy-lambda/master/resources/fnative && chmod +x fnative && sudo mv fnative /usr/local/bin
   ```
2. Copy `hello-lambda` from the examples and you have everything to experiment

## Installation
Add the following to your `:dependencies` in `project.clj`:

  ```
  [fierycod/holy-lambda "0.0.1"]
  ```

## License
License
Copyright © 2019 Karol Wojcik

Released under the MIT license.

## Acknowledges
- Daria - Thank you that you were always beside me, fighting for me when I had no faith and energy.
- @KrzysztofTucholski - Thank you for boosting my ego
- @uswitch - Thank you for interests in AWS Lambda Functions and providing us the `lambada`
- @hjhamala - Thank you for sharing [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html) about native lambda functions. You have inspired me to write `holy-lambda`.
