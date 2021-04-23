# holy-lambda
Holy lambda is a micro framework which adds support for running Clojure on the [AWS Lambda](https://aws.amazon.com/lambda/) and might be used with arbitrary AWS Lambda deployment tool. Holy lambda provides very convinient environment compared to other tools such as [uswitch/lambada](https://github.com/uswitch/lambada) or [babashka-lambda](https://github.com/dainiusjocas/babashka-lambda) via simple, but powerful `Makefile` recipes eg. deployment is as easy as running `make make-bucket deploy` (for native lambda `make make-bucket native-deploy`).

# holy-lambda-babashka-layer
holy-lambda-babashka-layer is a special layer for targeting `babashka` runtime from `holy-lambda`. 

For small artifacts mark following dependencies in you build as provided:
- clojure
- holy-lambda-babashka-runtime
- holy-lambda-default-retriever

# How to update
## Updating to newest babashka version
Provide a PR with changed babashka version in Dockerfile and template.yml. Don't change layer semanticVersion.

## Additional pods or pods update
Provide a PR with changed versions in deps.edn of `:pods`. Add/update entries in template.yml. Don't change layer semanticVersion


