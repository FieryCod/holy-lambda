#!/usr/bin/env bash
set -euo pipefail

bash -c "rm -Rf holy-lambda-babashka-runtime && cd ../../../../modules/holy-lambda-babashka-layer/ && make build-artifact-in-place" && bash -c "mv ../../../../modules/holy-lambda-babashka-layer/holy-lambda-babashka-runtime.zip ." && bash -c "mkdir -p holy-lambda-babashka-runtime && mv holy-lambda-babashka-runtime.zip holy-lambda-babashka-runtime && cd holy-lambda-babashka-runtime && unzip holy-lambda-babashka-runtime.zip && rm -Rf holy-lambda-babashka-runtime.zip"
