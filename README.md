<p align="center">
  <a href="https://fierycod.github.io/holy-lambda" target="_blank" rel="noopener noreferrer">
    <img src="docs/media/logo.png?raw=true" alt="holy-lambda logo">
  </a>
</p>

<p align="center">
  <a href="https://github.com/FieryCod/holy-lambda/actions/workflows/ci.yml"><img src="https://github.com/FieryCod/holy-lambda/actions/workflows/ci.yml/badge.svg"></a>
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-green.svg"></a>
</p>

The extraordinary simple, performant, and extensible custom AWS Lambda runtime for Clojure.

**Holy Lambda supports multiple backends**
  - [Babashka](https://github.com/babashka/babashka),
  - Native Clojure (GraalVM compiled), 
  - Clojure (much faster than official AWS Java runtime),

## Goals
  - **Low cold starts** - Clojure goes fast on AWS Lambda.
  - **Multiple backends support** - Unified runtime for Clojure.
  - **Minimal API** - Just stuff that gets the job done.

## Non-Goals
  - **Tight integration with deployment tools** - I don't want to do this.
  - **ClojureScript support** - there is no additional value HL may provide for Clojurescript. Use either [nbb](https://github.com/babashka/nbb) or [shadow-cljs](https://github.com/thheller/shadow-cljs) with advanced compilation.

## Companies & Inviduals using Holy Lambda?
  - [nextdoc.io](https://nextdoc.io) - 6 native lambdas: api-gateway custom authorizer, file access control, openapi data source etc.
  - [scalably.ai](https://scalably.ai) - 14 native lambdas: xml transformations, sftp interactions, message routing, encryption etc.
  - [retailic](https://retailic.com/) - native lambda that uses compatibility layer with ring, regular Clojure api on ARM64 
  - [day8](https://www.day8.com.au/)
  
## Extensions
  - [holy-lambda-ring-adapter](https://github.com/FieryCod/holy-lambda-ring-adapter) - Supports running full featured Ring apps on AWS Lambda

## Resources
- [Documentation](https://fierycod.github.io/holy-lambda)
- [HL Builder Images](https://github.com/FieryCod/holy-lambda/pkgs/container/holy-lambda-builder)
- [GraalVM & Clojure](https://github.com/clj-easy/graal-docs)

## Current Version 
[![Clojars Project](https://img.shields.io/clojars/v/io.github.FieryCod/holy-lambda?labelColor=283C67&color=729AD1&style=for-the-badge&logo=clojure&logoColor=fff)](https://clojars.org/io.github.FieryCod/holy-lambda)

## Getting Help 
[![Get help on Slack](http://img.shields.io/badge/slack-clojurians%20%23holy--lambda-97C93C?labelColor=283C67&logo=slack&style=for-the-badge)](https://clojurians.slack.com/channels/holy-lambda)

## License
Copyright © 2021 Karol Wojcik aka Fierycod

Released under the MIT license.
