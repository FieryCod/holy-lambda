# Changelog

## Unreleased
- [bb tasks] Add additional describe:stacks step for stack:destroy, since sometimes stack cannot be destroyed.
- [bb tasks] Allow to force compilation on sources
- [holy-lambda] Unify multiHeaders, headers conversion for both local and AWS environment. Breaking change if you were getting the headers from request.

## 0.1.54 (06-06-2021)
- [bb tasks] Fix adding Clojure/Pods deps for Babashka projects. For now all the the runtime will create a temporary template-modified.yml file which should be ignored.
- [bb layer] 0.0.34 Fix doubled holy-lambda dependency. Update layer to use the latest holy-lambda library. 
- [bb layer] Fix edge case when babashka layer is published without the `bb` executable.
- [holy-lambda] Add html response support
- [holy-lambda] Automatically parse :body json string to PersistentMap
- [bb tasks] Check if aws command is available. If not then exit early!
- [bb tasks] Fix `CLJ_ALIAS` checking
- [readme,docs] Refine docs. Add documentation on using local libraries
- [holy-lambda-template] Apply correct formatting, use up to 6 characters for sha to prevent gen of incorrect bucket names

## 0.1.50 (03-06-2021)
*Changes*
- [bb tasks] Remove :build-variant. Replaced by [:docker](https://github.com/FieryCod/holy-lambda/blob/master/modules/holy-lambda-template/resources/leiningen/new/holy_lambda/bb.edn#L15) section
- [bb tasks] Change native-configurations path. User should be able to modify the configuration which is now generated at resources/native-configurations
- [bb tasks] Allow runtime override when using :native runtime. You can now invoke lambda in :java runtime when :native runtime is declared in bb.edn:
  ```
  bb stack:invoke :runtime :java
  ```
- [bb tasks] Allow nil value for :native-deps option
- [bb tasks] Add support for setting :docker:volumes. This is crucial when working with local libraries, since HL runs tasks in docker context!
- [bb layer] Bump layer version to 0.0.32 which includes babashka v0.4.4
- [bb tasks] Commands bb bucket:create and bb bucket:remove can now create/remove specified bucket (not only the stack bucket) when :name parameter is used
- [bb tasks] Inform the user when docker image is not downloaded. This makes HL more user friendly.
- [bb tasks] Don't AOT whole project when uberjaring. AOT compile only the main class.
- [bb tasks] Add support for deps.edn aliases in bb.edn. Useful for local libraries

## 0.1.49 (21-05-2021)
*Changes*
- [bb tasks] Fix bb stack:sync which was failing on the first run
- [bb tasks] Add support for the custom profile in :infra
- Update babashka layer to version 0.4.1
- [bb tasks] Add an automated way of managing layer (update/downgrade). The new way is not compatible with the previous one. Please remove your old babashka layers stack and run `bb stack:sync`.
- [docs] Add documentation for getting started guide, refine template (entirely done by @lowecg. Thank you so much!)
- [holy-lambda] Change the order of execution for :leave interceptors. Order is the same as for Pedestal Interceptors now :)
- [bb tasks] Add validation for docker, AWS setup. Now holy-lambda will fail early with the helpful message if either AWS or docker is not properly set up.
- [bb tasks] Region is now derived from the profile if exists. A region in bb.edn overrides default region
- [bb tasks] Commands which depend on stack:sync are now restricted if the project is not synced
- [bb tasks] stack:invoke, stack:deploy, stack:api now support setting :params for AWS SAM parameters-overrides.

  **Example**:
  ```
  bb stack:invoke :params "{:SomeParameter \"Hello World\"}"
  ```

## 0.1.45 (05-05-2021)
*Changes*
- [docker] ce tag is used internally by bb tasks. Projects which use Makefiles should either switch to bb tasks or use image without tag
- [bb tasks] introduce bb tasks to manage stack, infrastracture and runtimes
- [bb layer] don't require uberjar. Provide sources as is.
- [holy-lambda] Fixup runtime error handling
- [holy-lambda] Fixup native-configuration gen
- [holy-lambda-template] Drop support for Makefile and Leiningen 
- [holy-lambda-babashka-shim] Provide a shim for jsonista and AWS interfaces so that babashka runtime works flawlessly

## 0.1.21 (21-04-2021)
*Changes*
- Add new docker tags: 
  - ce - for community edition
  - ce-ci - suitable for usage in docker to deploy with sam. Changes in Makefile are in progress
  - ee - you have to provide artifacts and build it on your own using Dockerfile provided in docker/ee
- Speedup interceptors on java/native runtime
- Provide Babashka runtime. Semantics of holy-lambda stay the same. For now bb is included in a zip which is totally lame. If more people will like this runtime then I will provide: babashka as a layer, minimal holy-lambda jar without jsonista (I would rather babashka to use jsonista instead of cheshire)
- Adapt template to allow babashka usage
- Add slack badge

## 0.1.15 (19-04-2021)
*Changes*
- Add partial support for GraalVMEE (PGO optimizations in progress)
- Move code to separate packages
- Add interceptors support
- Make call to lambda a private function
- Rename fierycod.holy-lambda.core/gen-main to fierycod.holy-lambda.native-runtime/entrypoint (one targeting java runtime should not depend on native-runtime namespace)
- Provide agent/in-context to ease generation of native-configurations
- Unify responses. Response should be a valid map or nil for event ACK. Text/plain should be send via response/text.
- Add some docs
- Fixup tests

## 0.1.2 (05-04-2021)
*Changes*
- Add full support for async-retriever (look at examples/hello-world & https://github.com/FieryCod/holy-lambda-async-retriever)
- Remove unecessary steps for CircleCI
- Add tests. Allow async handlers (future, promise, map)
- Add CHANGELOG.md
- Provide response utils based on ring-core. Add example of redirect

## 0.1.1 (05-04-2021) (Breaking change) 
Handler definition has been changed from `[event,context] -> response` to `[request] -> response`.

*Changes*
- Fixup reflection
- Add deps.edn; Pregenerate routes; Split runtimes; Add tests
- Switch to rum based macro. Add hook support based on rum hook
- Switch from data.json to jsonista + fixes
- Use Clojure in :scope "provided"
- Handle not existing invocation-id
- (Breaking change) Don't keywordize envs. Tidy up runtime.
- Use only one arity lambdas
- Remove logging system. User should use 3rd party logging implementations
- Update coordinates to docker image
- Allow pass of --static parameter with --libc=musl
- remove 'choly' (#22)
- Revert removal of Dockerfile. Bump graalvm
- Add install-graalvm command
- Remove Node.js dependency
- Switch from joker -> clj-kondo
- Example makefile improvements (#19)
- chore(deps): bump bleach from 3.1.0 to 3.1.1 in /src/python/choly (#15)
- chore(deps): bump handlebars from 4.1.2 to 4.5.3 (#14)
- test(agent): fix tests by correctly sorting payloads (thanks @vemv)
- chore(documentation,release-script): reformat documentation & fix releaser

## 0.0.7 (18-07-2019)

*Changes*
- chore(documentation) Introduce the first draft of documentation
- fix(custom_runtime,choly) Keywordize hashmap & fix CodeUri gen
- chore(test,readme.md): Change badge & attempt to fix test run
- chore(package.json): bump git-cz dependencies

## 0.0.5 (17-07-2019)

*Changes*
- feat(choly,agent): Add basic choly cli tool & agent executor
- Initial repro for https://github.com/oracle/graal/issues/1367
- refactor(core,agent): Move the core to separate namespaces & implement draft of GraalVM agent
- fix(cljdoc): Fix cljdoc build
- fix(circleci): Fix CircleCi configuration

## 0.0.2 (09-06-2019)

*Changes*
- ci(circleci): Add CircleCi automation
- feat(sqs-example): Add reproducible error in sqs-example for GraalVM team
- feat(example, runtime, version): Add sqs example & bump the version of Clojure dependency
- feat(graalvm,dockerfile): Change the default version of GraalVM to the latest stable one
- feat(runtime,makefiles,readme): Add custom logger & log the runtime fatals
- feat(runtime,makefiles): Add workable lambda for both runtimes
- feat(runtime,fnative): Add a very first version of holy-lambda
