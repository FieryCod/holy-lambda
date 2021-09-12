
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

The extraordinary simple, performant, and extensible custom AWS Lambda runtime for Clojure.

**Holy Lambda supports multiple backends**
  - [Babashka](https://github.com/babashka/babashka),
  - Native Clojure (GraalVM compiled), 
  - Clojure (much faster than official AWS Java runtime),

**Incoming**
  - [nbb](https://github.com/borkdude/nbb)
  - ClojureScript

## Goals
  - **Low cold starts** - Clojure goes fast on AWS Lambda!
  - **Multiple backends support** - Unified runtime for Clojure/script!
  - **Minimal API** - Just stuff that gets the job done!

## Non-Goals
  - **Tight integration with deployment tools** - I don't want to do this!

## Companies & Inviduals using Holy Lambda?
  - [nextdoc.io](https://nextdoc.io) - 6 native lambdas: api-gateway custom authorizer, file access control, openapi data source etc.
  - [scalably.ai](https://scalably.ai) - 14 native lambdas: xml transformations, sftp interactions, message routing, encryption etc.

## Documentation
The holy-lambda documentation is available [here](https://fierycod.github.io/holy-lambda).

## Current Version 

[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda?labelColor=283C67&color=729AD1&style=for-the-badge&logo=clojure&logoColor=fff)](https://clojars.org/io.github.FieryCod/holy-lambda)

## Getting Help 

[![Get help on Slack](http://img.shields.io/badge/slack-clojurians%20%23holy--lambda-97C93C?labelColor=283C67&logo=slack&style=for-the-badge)](https://clojurians.slack.com/channels/holy-lambda)

## License
Copyright © 2021 Karol Wojcik aka Fierycod

Released under the MIT license.
