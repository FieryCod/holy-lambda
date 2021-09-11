
<p align="center">
  <a href="https://day8.github.io/re-frame" target="_blank" rel="noopener noreferrer">
    <img src="docs/media/logo.png?raw=true" alt="re-frame logo">
  </a>
</p>

<p align="center">
  <a href="https://circleci.com/gh/FieryCod/holy-lambda/tree/master"><img src="https://circleci.com/gh/FieryCod/holy-lambda/tree/master.svg?style=svg"></a>
  <a href="https://dev.azure.com/VetHelpAssistant/holy-lambda/_build/latest?definitionId=2&branchName=master"><img src="https://dev.azure.com/VetHelpAssistant/holy-lambda/_apis/build/status/FieryCod.holy-lambda?branchName=master"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-green.svg"></a>
</p>

The extraordinary simple, performant, and extensible custom AWS Lambda runtime.

**Holy Lambda supports multiple backends**
  - Babashka
  - Native Clojure runtime
  - Clojure custom runtime (much faster than official AWS Java runtime)
  - (incoming) Node.js runtime for Clojurescript

## Who’s using Holy Lambda?
- [nextdoc.io](https://nextdoc.io) - 6 native lambdas: api-gateway custom authorizer, file access control, openapi data source etc.
- [scalably.ai](https://scalably.ai) - 14 native lambdas: xml transformations, sftp interactions, message routing, encryption etc.

## Documentation
The holy-lambda documentation is available [here](https://fierycod.github.io/holy-lambda).

## Current Version 

[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda?labelColor=283C67&color=729AD1&style=for-the-badge&logo=clojure&logoColor=fff)](https://clojars.org/io.github.FieryCod/holy-lambda)

## Getting Help 

[![Get help on Slack](http://img.shields.io/badge/slack-clojurians%20%23holy--lambda-97C93C?labelColor=283C67&logo=slack&style=for-the-badge)](https://clojurians.slack.com/channels/holy-lambda)

## Thanks to
- Daria - Thank you that you were always beside me, fighting for me when I had no faith and energy.
- @KrzysztofTucholski - Thank you for boosting my ego
- @uswitch - Thank you for interests in AWS Lambda Functions and providing us the `lambada`
- @hjhamala - Thank you for sharing [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html) about native lambda functions. You have inspired me to write `holy-lambda`.
- Rum - deflambda parse mechanism is adapted from rum.
- Ring - code from fierycod.holy-lambda.response is adapted from ring-core. 
- re-frame - README of the project is heavily inspired by re-frame

## License
Copyright © 2021 Karol Wojcik aka Fierycod

Released under the MIT license.
