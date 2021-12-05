# Stable releases
## Libraries
| Artifact name                                    | Version | Purpose                                                        |
|--------------------------------------------------|---------|----------------------------------------------------------------|
| io.github.FieryCod/holy-lambda                   | 0.6.4   | Core library / Custom runtime implementation                   |
| io.github.FieryCod/holy-lambda-default-retriever | 0.5.0   | Built in library supporting regular responses*                 |
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
  | `e6c47274a2bfc7576a9da0ccdbc079c1e83bee17` |

  ```clojure bb.edn
  {:deps 
   {io.github.FieryCod/holy-lambda-babashka-tasks
    {:git/url     "https://github.com/FieryCod/holy-lambda"
     :deps/root   "./modules/holy-lambda-babashka-tasks"
     :sha         "<STABLE_RELEASE>"}}}
  ```
## CLI & CI/CD Docker Images
All available images: https://github.com/FieryCod/holy-lambda/pkgs/container/holy-lambda-builder/versions

##### Recommended
  | Image name                                                 | GraalVM  | Architecture | Java |
  |------------------------------------------------------------|----------|--------------|------|
  | ghcr.io/fierycod/holy-lambda-builder:aarch64-java11-21.3.0 | *21.3.0* | ARM64        | *11* |
  | ghcr.io/fierycod/holy-lambda-builder:amd64-java11-21.3.0   | *21.3.0* | AMD64        | *11* |

#### Using different Docker image in CLI
  ```clojure bb.edn
  {:holy-lambda/options 
   {:docker 
    {:image "<IMAGE_PLUS_TAG>"}}}
  ```
  
## Babashka Lambda Layers
### Deprecated
  | Name                         | Backend         | Source                                                                                         | Serverless Repo                                                                                                   | Version |
  |------------------------------|-----------------|------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|---------|
  | (DEPRECATED) holy-lambda-babashka-runtime | Babashka v0.6.0 | [link](https://github.com/FieryCod/holy-lambda/tree/master/modules/holy-lambda-babashka-layer) | [link](https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime) | 0.5.1   |

### Recommended
  | Deployable ServerlesRepo Artifact                                                                                       | Babashka | Architecture | Version |
  |-------------------------------------------------------------------------------------------------------------------------|----------|--------------|---------|
  | [link](https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime-amd64) | *21.3.0* | AMD64        | 0.6.8   |
  | [link](https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime-arm64) | *21.3.0* | ARM64        | 0.6.8   |
