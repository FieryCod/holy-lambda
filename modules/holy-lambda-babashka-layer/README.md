[![Slack](https://img.shields.io/badge/Slack-holy--lambda-blue?logo=slack)](https://clojurians.slack.com/messages/holy-lambda/)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# holy-lambda
Holy lambda is a micro framework which adds support for running Clojure on the [AWS Lambda](https://aws.amazon.com/lambda/) and might be used with arbitrary AWS Lambda deployment tool. To ease the development holy-lambda comes with special `bb tasks` recipes and provides very convinient environment compared to other tools such as [uswitch/lambada](https://github.com/uswitch/lambada) or [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda) via simple, but powerful `bb tasks` recipes.

# holy-lambda-babashka-layer
It's a special layer for targeting `babashka` runtime from `holy-lambda` microframework. 

For small artifacts mark following dependencies in you build as provided:
- clojure
- holy-lambda-babashka-shim
- holy-lambda-default-retriever (In babashka `clojure.core.async/<!` ~= `clojure.core.async/<!!`)

# Holy Lambda vs Babashka Lambda

| holy-lambda                                            | babashka-lambda             |
|--------------------------------------------------------|-----------------------------|
| multiple handlers exported                             | single handler exported     |
| asynchrounous handlers (chan, promise, thread)         | only synchronous handlers   |
| supports pods                                          | does not support pods usage |
| automatic body json string -> PersistentMap conversion | no conversion               |

## Babashka as a holy-lambda runtime
[Babashka](https://github.com/babashka/babashka) runtime provides interactive development environment. There is no need for compiling the sources since those are provided as is to `AWS SAM`.

| Runtime   | Cold start | Performance | Artifacts size   | Memory Consumption | Interactive | Compile time |
|-----------|------------|-------------|------------------|--------------------|-------------|--------------|
| :native   | low        | high        | high     >= 16mb | low                | No          | very long    |
| :babashka | low        | moderate    | low      >= 50kb | low                | Yes         | no compile   |
| :java     | high       | high        | moderate >= 12mb | high               | No          | long         |


If you're not interested in trying holy-lambda out then you can check very minimal babashka runtime [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda).

# More info
- [Examples](https://github.com/FieryCod/holy-lambda/tree/master/examples/bb)
<!-- - [Documentation](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT/doc/readme) -->
- [AWS Serverless Repository](https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime)
