# What it is?

Holy lambda is a micro framework which adds support for running Clojure on the [AWS Lambda](https://aws.amazon.com/lambda/) and might be used with arbitrary AWS Lambda deployment tool. Holy lambda provides very convenient environment compared to other tools such as [uswitch/lambada](https://github.com/uswitch/lambada) or [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda) via simple, but powerful `Makefile` recipes eg. deployment is as easy as running `make make-bucket deploy` (for native lambda `make make-bucket native-deploy`).

*Holy lambda supports*
- interceptors
- ring request, response model
- async handlers

## Java runtime
Prior work towards targeting Java runtime was done by [uswitch/lambada](https://github.com/uswitch/lambada), but lacked being convienient. Holy lambda in the other hand is very convenient and does things which lambada lacked:

- full class identifier is created during macroexpansion of `deflambda` 
- request is automatically slurped and converted to a map
- response mimics Ring therefore you don't have to write to `OutputStream`. 
- supports interceptors
- converts ctx

## Native runtime
I've started experimenting with native runtime around May 2019 inspired by @hjhamala blog [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html). At that time Clojure community just started experimenting with GraalVM, and there was not such great tools as [babashka](https://github.com/babashka/babashka), therefore holy-lambda is not babashka based. There are some benefits of not using babashka and some tradeoffs which have to be understood before taking a right decision.

*Benefits*
- lambdas will be faster in general, because code is not interpreted (benchmarks in-progress)
- use any GraalVM Clojure compatible library. Not only one from limited pool of babashkas pods
- target both Java & native runtime
- mix GraalVM supported languages freely (polyglot)
- use Profile-Guided Optimizations on GraalVM EE [PGO](https://www.graalvm.org/reference-manual/native-image/PGO/)

*Tradeoffs*
- you have to generate native-configurations for GraalVM (automated by running `make native-gen-conf`)
- GraalVM compilation is long - for development use Java runtime with `sam invoke` or `make dry-api`
- adding and using new libary is not always easy when compiling to native, some extra know-how about GraalVM is needed

If you're not interested in trying holy-lambda out then you can check babashka runtime :) [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda).
