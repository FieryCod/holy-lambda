# Holy Lambda
[![Clojars Project](https://img.shields.io/clojars/v/fierycod/holy-lambda.svg?logo=clojure&logoColor=white)](https://clojars.org/fierycod/holy-lambda)
[![CircleCI](https://circleci.com/gh/FieryCod/holy-lambda/tree/master.svg?style=svg)](https://circleci.com/gh/FieryCod/holy-lambda/tree/master)
[![codecov](https://codecov.io/gh/FieryCod/holy-lambda/branch/master/graph/badge.svg)](https://codecov.io/gh/FieryCod/holy-lambda)
[![cljdoc badge](https://cljdoc.org/badge/fierycod/holy-lambda)](https://cljdoc.org/d/fierycod/holy-lambda/0.0.5)
[![pipi badge](https://img.shields.io/pypi/v/choly)](https://pypi.org/project/choly/)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

---

Tiny native AWS Custom Lambda Runtime which fulfills your needs!

## What it does?
It allows you to write one code which might run either on Official Java AWS Runtime (if you don't care about speed or you want to test your lambdas)
or on Native Custom AWS Runtime built into your codebase.

[Jump here](https://cljdoc.org/d/fierycod/holy-lambda/CURRENT/doc/1-01-installation) to learn more and start the journey with the Holy Lambda.

## Quickstart
```
lein new holy-lambda <your-project-name>
```

## License
Copyright © 2019 Karol Wojcik aka Fierycod

Released under the MIT license.

## Acknowledges
- Daria - Thank you that you were always beside me, fighting for me when I had no faith and energy.
- @KrzysztofTucholski - Thank you for boosting my ego
- @uswitch - Thank you for interests in AWS Lambda Functions and providing us the `lambada`
- @hjhamala - Thank you for sharing [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html) about native lambda functions. You have inspired me to write `holy-lambda`.
