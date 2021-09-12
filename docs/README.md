# Rationale
Writing Clojure handlers for AWS Lambda is not always as simple as it should be. For many years, Clojure developers used libraries such as `uswitch/lambada` as a glue between Java and Clojure with mediocre results due to the cold starts and high memory usage. 

**Cold starts**

To understand what causes cold starts, we have to know how Clojure distributes program classes. 

When Clojure code is compiled and packed to uberjar, every function expands to a class. Imagine that you have the `main` function in your namespace, which you then uberjar for later execution. In standalone uberjar, you will find all Clojure core functions and your `main` function compiled to classes. Upon `java -jar` execution, both your `main` class, and Clojure core classes are load. Loading the classes is mostly what makes high cold start times.

For only a single Java AWS Lambda handler class, the cold start is around ~1s. For Clojure on official AWS Lambda Java runtime  the cold start is between 8-12on a 2GB memory-sized environment. The difference between cold starts comes from the number of classes load upon startup (Clojure > 100, Java > 1). 

*As a remedy for the said issue the new library has been developed called **Holy Lambda***.

----

**Holy Lambda (HL)**
HL is a micro-framework for running Clojure on the [AWS Lambda](https://aws.amazon.com/lambda/). HL is a deployment tool independent and supports multiple Clojure backends via a powerful and small core custom runtime. 

HL supports at the time of writing three Clojure backends. 

1. Java/Clojure with [Tiered](https://aws.amazon.com/blogs/compute/increasing-performance-of-java-aws-lambda-functions-using-tiered-compilation/) compilation distributed as a Docker image,

2. GraalVM native Clojure. Utilizes GraalVM native image to provide fast startup and a low memory footprint. The tradeoff in using native runtime is the steep learning curve of GraalVM.

3. [Babashka](https://github.com/babashka/babashka) backend which is both fast and memory efficient. Babashka supports interactive development. There is no need for compiling the sources since those are provided as is to `AWS SAM`. 
   
   A great fit for beginners. It's fast enough, although it's not as fast as the native runtime. The tradeoff of using babashka is that not all of the Clojure language features are supported.

## Backends quick comparison
<div align="center">

| Runtime   | Cold start | Performance | Artifacts size         | Memory Consumption | Interactive | Compile time | Beginners friendly? | Package Type  |
|-----------|------------|-------------|------------------------|--------------------|-------------|--------------|---------------------|---------------|
| :native   | low        | high        | high     >= 16mb       | low                | No          | very long    | no                  | Zip/Docker    |
| :babashka | low        | moderate    | low      >= 50kb       | low                | Yes         | no compile   | yes                 | Zip/Docker    |
| :clojure  | moderate   | high        | depends on docker img  | low                | No          | long         | yes                 | Docker        |
</div>
