# Rationale
Writing Clojure handlers for AWS Lambda is not always as simple as it should be. For many years, Clojure developers used libraries such as `uswitch/lambada` as a glue between Java and Clojure with mediocre results due to the cold starts and high memory usage. 

**Cold starts**
To understand what causes cold starts, we have to know how Clojure distributes program classes. 

When Clojure code is compiled and packed to uberjar, every function expands to a class. Imagine that you have the `main` function in your namespace, which you then uberjar for later execution. In standalone uberjar, you will find all Clojure core functions and your `main` function compiled to classes. Upon `java -jar` execution, both your `main` class, and Clojure core classes are load. Loading the classes is mostly what makes high cold start times.

For only a single Java AWS Lambda handler class, the cold start is around ~1s. For Clojure, the cold start starts from ~8s on a 2GB memory-sized environment. The difference between cold starts comes from the number of classes load upon startup (Clojure > 100, Java > 1). 

**Holy Lambda (HL)**
HL is a micro-framework for running Clojure on the [AWS Lambda](https://aws.amazon.com/lambda/). HL is a deployment tool independent, although ships with `bb tasks` for convenience.
HL supports at the time of writing three AWS Lambda runtimes. The first one is Clojure/Java runtime that is good for development, but a bad idea for production due to the cold starts and high memory usage of Java-based lambdas. 
The second one is custom native runtime which utilizes GraalVM native image to provide fast startup and a low memory footprint. The tradeoff in using native runtime is the steep learning curve of GraalVM.
The last one is the `babashka` runtime which is both fast and memory efficient. Babashka supports interactive development, and it's a great fit for beginners. It's fast enough, although it's not as fast as the native runtime. The tradeoff of using babashka is that not all of the Clojure language features are supported.

Holy lambda provides very convenient environment compared to other tools such as [uswitch/lambada](https://github.com/uswitch/lambada) or [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda) via simple, but powerful `bb tasks` recipes eg. deployment is as easy as running `bb stack:sync && bb stack:compile && bb stack:pack && bb stack:deploy`).

**Features**
- interceptors
- ring request, response alike model
- async handlers

## Java runtime
Prior work towards targeting Java runtime was done by [uswitch/lambada](https://github.com/uswitch/lambada), but lacked being convenient and fast. Holy lambda in the other hand is very convenient extremely fast, and does things which lambada lacked:

- full class identifier is created during macro expansion of `deflambda` 
- request is automatically slurped and converted to a map
- response mimics Ring therefore you don't have to write to `OutputStream`. 
- supports interceptors
- converts ctx

## Native runtime
I've started experimenting with native runtime around May 2019 inspired by @hjhamala blog [post](https://dev.solita.fi/2018/12/07/fast-starting-clojure-lambdas-using-graalvm.html). It was clear to me that there is huge potential in GraalVM which could be embraced in Clojure on AWS Lambda. 

**Benefits**
- lambdas will be faster in general, because code is not interpreted (benchmarks in-progress)
- use any GraalVM Clojure compatible library. Not only one from limited pool of babashka's pods
- target both Java & native runtime
- mix GraalVM supported languages freely (polyglot)
- use Profile-Guided Optimizations on GraalVM EE [PGO](https://www.graalvm.org/reference-manual/native-image/PGO/)

**Tradeoffs**
- you have to generate native-configurations for GraalVM (automated by running `bb native:conf`)
- GraalVM compilation is long - for development use Java runtime with `sam invoke` or `bb stack:invoke`
- adding and using new library is not always easy when compiling to native, some extra know-how about GraalVM is needed

## Babashka runtime
[Babashka](https://github.com/babashka/babashka) runtime provides interactive development environment. There is no need for compiling the sources since those are provided as is to `AWS SAM`. More info [here](https://github.com/FieryCod/holy-lambda/blob/master/modules/holy-lambda-babashka-layer/README.md).
If you feel overhelmed by holy-lambda ecosystem you can check very [this](https://github.com/dainiusjocas/babashka-lambda) minimal babashka runtime.

## Runtime comparison

| Runtime   | Cold start | Performance | Artifacts size   | Memory Consumption | Interactive | Compile time | Beginners friendly? |
|-----------|------------|-------------|------------------|--------------------|-------------|--------------|---------------------|
| :native   | low        | high        | high     >= 16mb | low                | No          | very long    | no                  |
| :babashka | low        | moderate    | low      >= 50kb | low                | Yes         | no compile   | yes                 |
| :java     | high       | high        | moderate >= 12mb | high               | No          | long         | yes                 |

PS: I don't claim the AWS Lambda or Serverless is superior to traditional servers, and I will never do. AWS Lambda is a viable choice in only a limited of tasks. Still, though, it's worth having Clojure support for low memory, high-performance Clojure handlers on it, and that's why holy-lambda exists.
