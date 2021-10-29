#  Native backend
  Native backend is the most performant in terms of cold starts option for Clojure on AWS Lambda. The backend is compiled with the user code via [GraalVM Native Image](https://www.graalvm.org/reference-manual/native-image/) (highly recommend to read, native-image manual first) tool, that run the Java code on SubstrateVM. Majority of the Clojure libraries work well with Native Image, some require additional configuration, which is not so hard to do if you understand the error messages. Only nominal Clojure libraries are incompatible with GraalVM thus require some patching. 
  
  One of the examples of such incompatible (at the time of writing) library is [nano-id](https://github.com/zelark/nano-id/), that heavily uses `SecureRandom` in static fields that would force the GraalVM to initialize the `SecureRandom` on image build phase and cache the seeds. Of course GraalVM native-image is  smart enough and already prevents the compilation for such cases. Otherwise `SecureRandom` would be neither secure nor random.
  
## Native configuration
  Native image tool creates a binary that includes application classes, dependencies classes, runtime libraries, application resources, and statically linked native code and most importantly the SubstrateVM, that executes the user code. 
  
  To optimize the binary size, and for performance reasons code provided to native-image must should be reflection free, and all the assets must be provided via additional `resource-config.json`. If the provided code uses the reflection, then either the compilation will fail or during execution of the program an `NoClassException` will be thrown. GraalVM native-image allows for `Class.forName` usage, but the classes used reflectively have to declared in `reflection-config.json`. Of course the more you declare in configuration the bigger the binary size is, therefore you should always aim to provide only minimal configuration required.
  
  There are two more configuration files left, that are less commonly used in Clojure world. One is `serialization-config.json` in which you have to specify the classes, that are serialized during the execution of the program. The example of library that requires the additional `serialization-config.json` is [nippy](https://github.com/ptaoussanis/nippy). Although nippy works great in native-image when regular Clojure structures are `freezed` or `thawed`, it fails to freeze the instance of `ExceptionInfo`, and Java `Throwable`'s types. 
  
  > :information_source: The additional support for Nippy's freeze of Java/Clojure exceptions is distributed via [clj-easy/graal-config](https://github.com/clj-easy/graal-config).
  
  Another one is `jni-config.json`, that has to be provided only if the code uses native JDK code. Regular JNI can be successfully traced via GraalVM [native-image Java agent](https://www.graalvm.org/reference-manual/native-image/Agent/), but JNA requires some additional configuration, and it's troublesome to specify the full list of the natively provided JNA functions.
  
  > :information_source: For JNA example you should take a look into [this](https://github.com/amahfouz1/jna-graalvm)
  
  > :information_source: If you find incompatibility between Clojure library and GraalVM native-image, please report library name here: [clj-easy/graal-config](https://github.com/clj-easy/graal-config)

## Reflection free Clojure programs
  To ensure reflection free program set the dynamic `*warn-on-reflection*` Clojure var. Highly encourage to set the var in the every possible namespace just below it's declaration.
  
  **Example**
  
  ```clojure
  (ns example.core
    (:require ...)
    (:import ...))

  (set! *warn-on-reflection* true)
  
  ;; Rest of the program
  ...
  ```
  
## Native Image Java Agent

  HL provides an additional command `hl:native:conf`, that generates a native configuration via official [native image java agent](https://www.graalvm.org/reference-manual/native-image/Agent/).
  
  Agent tracing code path can be configured via either `agent-payloads` or `in-context` calls. 
  
  > :information_source:  One should note, that generated native configuration is automatically passed to `hl:native:executable`.

## Agent payloads

 This is a great option if you would like to cover the whole execution path for the lambda handler. Agent paylods are the set of `edn` files placed in a special `resources/native-agents-paylods` directory. During the `hl:native:conf` HL invokes a specified in file lambda with the `HLEvent` and `HLContext`.

 **Example of the file**:
 
  ```clojure
  {:name "example.core.Handler",
   :request {:event {},
             :ctx {}},
   :propagate false}
  ```
 
 | Option       | Description                                                                                                                                           |
 |--------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
 | `:name`      | Qualified path of Lambda Handler                                                                                                                      |
 | `:request`   | Aggregates HLEvent & HLContext                                                                                                                        |
 | `:event`     | [HLEvent](http://localhost:3000/#/model?id=hlevent) - depends on event provider. Highly encourage to generate an event via `sam local generate-event` |
 | `:ctx`       | [HLContext](http://localhost:3000/#/model?id=hlcontext)                                                                                               |
 | `:propagate` | Should stop the agent when invocation error occurs?                                                                                                   |

 EDN's are alphabetically sorted before execution. A typical way of preserving sort of the payloads is by specifying the number prefix like `1-api.edn`, `2-sqs.edn`. 
 
## Agent in-context calls

  Using `fierycod.holy-lambda.agent/in-context` macro is the easier way to trigger tracing. The code in macro is executed only during `hl:native:conf` phase. Content of the macro is completely trimmed in the production build. 
  
  **Example**
  
  ```clojure
  (ns ...)
  
  (defn Lambda
   [request]
   ;; I can be used in functions too, however the function has to be called
   (fierycod.holy-lambda.agent/in-context 
    (call-1)
    (call-2)
    (call-3)
    (call-n))
   {:body nil
    :statusCode 200})
  
  ;; Initialize the runtime loop
  (h/entrypoint [#"Lambda"])
    
  (fierycod.holy-lambda.agent/in-context 
    (call-1)
    (call-2)
    (call-3)
    (call-n))
  ```
