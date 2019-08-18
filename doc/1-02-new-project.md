# Setup new project

In order to generate a new project you can use `holy-lambda-template`. Just invoke:
```
lein new holy-lambda example-project && cd example-project
```

After that please make sure that you have the latest version of `choly`, `aws`, `sam`
command utilities installed. If you find it difficult then you may find this [guide](https://cljdoc.org/d/fierycod/holy-lambda/CURRENT/doc/installation) helpful.

## Project structure
Generated project should have similiar tree of files/folders:
```
.
├── Makefile
├── project.clj
├── README.md
├── resources
│   ├── bootstrap
│   ├── local-event.json
│   ├── native-agents-payloads
│   │   └── 1-pass.json
│   └── native-deps
│       ├── cacerts
│       └── libsunec.so
├── src
│   └── example_project
│       └── core.clj
└── template.yml

5 directories, 10 files
```

**Makefile**

Starting from the very beginning you can see `Makefile` file which is used in conjuction with `choly` to provide *decent* support when it comes to:
- Maintenance of local envs for agent, native & java runtime (Automatically generated from `envs.json` via `make gen-envs`)
- Compilation of sources for both native (Holy Custom Runtime) & Java runtime (provided via `make compile` or `make compile native-compile` respectively)
- Packaging of the application for both runtimes (provided via `make pack` or `make native-pack`) to s3
- Management of the serverless stack via `destroy, make-bucket, deploy` commands
- Generation of native-configuration using `GraalVM Agent` (provided via `make gen-native-configuration`)
- Local development in HCR `make native-dry-api` in JRE `make dry-api`. You can as well directly call the lambda function via `make invoke` in JRE or `make native-invoke` in HCR.

**template.yml**

Next interesting part is `template.yml`. This file is used only by JRE and it can be translated during `make pack deploy` via `AWS Serverless Application Model (AWS SAM)` to Cloudformation file. If you want to target HCR, then you have to firstly translate `template.yml` to `native-template.yml` using `make gen-native-template`. By doing so
all resources with type `AWS::Serverless::Function` which doesn't have `Runtime` property specified or the ones with `Runtime` set to `provided` will have their `CodeUri` changed.

TODO: Describe native-deps, native-agents-payloads
