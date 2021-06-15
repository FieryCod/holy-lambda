# GraalVM EE with Holy Lambda user guide

## Preface
HL supports `GraalVM EE native-image` via custom `Dockerfile` recipe. Image out of the `Dockerfile` has to be manually built and tagged by the user. This requirement is a result of `GraalVM EE` licensing and I cannot/will not distribute `Docker` image, that includes any of the `GraalVM EE` component. 

## HL compatible docker image build
  1. Create a folder which will be a docker build context.
     ```sh
     mkdir -p graalvm-ee-hl && cd graalvm-ee-hl
     ```
  2. Download latest `Dockerfile.ee` from [here]
  


