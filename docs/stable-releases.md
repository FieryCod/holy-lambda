# Stable releases
## Libraries
| Artifact name                                    | Version | Purpose                                                                |
|--------------------------------------------------|---------|------------------------------------------------------------------------|
| io.github.FieryCod/holy-lambda                   | 0.5.0   | Core library / Custom runtime implementation                           |
| io.github.FieryCod/holy-lambda-default-retriever | 0.5.0   | Built in library supporting regular responses*                         |
| io.github.FieryCod/holy-lambda-async-retriever   | 0.5.0   | Additional support for `Channel<Map\|ByteArray\|nil>` response |

> :information_source: Regular valid responses*
> - `Map`
> - `nil`
> - `ByteArray`
> - `Future<Map|ByteArray|nil>`
> - `ClojurePromise<Map|ByteArray|nil>`

### Using library with `deps.edn`
Add to `deps.edn` an additional tuple at `:deps` property
 ```clojure deps.edn
 {:deps 
   {<ARTIFACT_NAME> {:mvn/version <ARTIFACT_VERSION>}}}
 ```
### Using `async` instead of `default` retriever
 ```clojure deps.edn
 {:deps 
  {io.github.FieryCod/holy-lambda                 {:mvn/version <ARTIFACT_VERSION>
                                                   :exclusions [io.github.FieryCod/holy-lambda-default-retriever]}
   ;; Add a async-retriever
   io.github.FieryCod/holy-lambda-async-retriever {:mvn/version <ARTIFACT_VERSION>}}}
 ```
 
## CLI
  | Stable release `:sha`                      |
  |--------------------------------------------|
  | `99fb7c975498945551e1ed8651f9017532be0a58` |
  ```clojure bb.edn
  {:deps 
   {io.github.FieryCod/holy-lambda-babashka-tasks
    {:git/url     "https://github.com/FieryCod/holy-lambda"
     :deps/root   "./modules/holy-lambda-babashka-tasks"
     :sha         "<STABLE_RELEASE>"}}}
  ```
## CLI & CI/CD Docker Images
  | Image name                    | Tag | GraalVM version            | Source                                                                            |
  |-------------------------------|-----|----------------------------|-----------------------------------------------------------------------------------|
  | fierycod/graalvm-native-image | ce  | *21.2.0*                   | [link](https://github.com/FieryCod/holy-lambda/blob/master/docker/Dockerfile.ce)  |
  | fierycod/graalvm-native-image | dev | *21.3.0-dev-20210910_2147* | [link](https://github.com/FieryCod/holy-lambda/blob/master/docker/Dockerfile.dev) |

#### Using different Docker image in CLI
  ```clojure bb.edn
  {:holy-lambda/options 
   {:docker 
    {:image "<IMAGE_PLUS_TAG>"}}}
  ```
  
## AWS Lambda Layers
  | Name                         | Backend         | Source                                                                                         | Serverless Repo                                                                                                   | Version |
  |------------------------------|-----------------|------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|---------|
  | holy-lambda-babashka-runtime | Babashka v0.6.0 | [link](https://github.com/FieryCod/holy-lambda/tree/master/modules/holy-lambda-babashka-layer) | [link](https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime) | 0.5.1   |
