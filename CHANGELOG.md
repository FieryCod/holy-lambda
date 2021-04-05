# Changelog

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
