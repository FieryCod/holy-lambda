# What is it?

Holy lambda is a micro framework which adds support for running Clojure on the [AWS Lambda](https://aws.amazon.com/lambda/) and might be used with arbitrary AWS Lambda deployment tool. Holy lambda provides very convenient environment compared to other tools such as [uswitch/lambada](https://github.com/uswitch/lambada) or [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda) via simple, but powerful `bb tasks` recipes eg. deployment is as easy as running `bb stack:sync && bb stack:compile && bb stack:pack && bb stack:deploy`).

*Holy lambda supports*
- interceptors
- ring request, response model
- async handlers

## Java runtime
Prior work towards targeting Java runtime was done by [uswitch/lambada](https://github.com/uswitch/lambada), but lacked being convenient. Holy lambda in the other hand is very convenient and does things which lambada lacked:

- full class identifier is created during macro expansion of `deflambda` 
- request is automatically slurped and converted to a map
- response mimics Ring therefore you don't have to write to `OutputStream`. 
- supports interceptors
- converts ctx

## Native runtime
I've started experimenting with native runtime around May 2019 inspired by @hjhamala blog [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html). At that time Clojure community just started experimenting with GraalVM, and there was not such great tools as [babashka](https://github.com/babashka/babashka), therefore holy-lambda is not babashka based. There are some benefits of not using babashka and some tradeoffs which have to be understood before taking a right decision.

*Benefits*
- lambdas will be faster in general, because code is not interpreted (benchmarks in-progress)
- use any GraalVM Clojure compatible library. Not only one from limited pool of babashka's pods
- target both Java & native runtime
- mix GraalVM supported languages freely (polyglot)
- use Profile-Guided Optimizations on GraalVM EE [PGO](https://www.graalvm.org/reference-manual/native-image/PGO/)

*Tradeoffs*
- you have to generate native-configurations for GraalVM (automated by running `bb native:conf`)
- GraalVM compilation is long - for development use Java runtime with `sam invoke` or `bb stack:invoke`
- adding and using new library is not always easy when compiling to native, some extra know-how about GraalVM is needed

## Babashka runtime
[Babashka](https://github.com/babashka/babashka) runtime provides interactive development environment. There is no need for compiling the sources since those are provided as is to `AWS SAM`.

| Runtime   | Cold start | Performance | Artifacts size   | Memory Consumption | Interactive | Compile time |
|-----------|------------|-------------|------------------|--------------------|-------------|--------------|
| :native   | low        | high        | high     >= 16mb | low                | No          | very long    |
| :babashka | low        | moderate    | low      >= 50kb | low                | Yes         | no compile   |
| :java     | high       | high        | moderate >= 12mb | high               | No          | long         |


If you're not interested in trying holy-lambda out then you can check very minimal babashka runtime :) [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda).
