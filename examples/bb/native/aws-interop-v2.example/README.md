# aws-interop-v2.example

An example of a Lambda that is deployed :native on AWS but runs in :java on localhost.

Slightly different from other Holy Lambda examples in that it's built to a `native:executable` locally or in CI and then deployed to AWS using the AWS CLI i.e. not the HL bb tasks.

Features:
1. 2 different S3 calls made by Lambda to provide data
2. Uses the AWS Java API via interop instead of the aws-api from Cognitect
3. Uses AWS client fakes in tests and mocks for native:conf invocations
4. provides a ring local http server that calls the handler using mocks that read from the local file-system

## Why not aws-api?

The more idiomatic Clojure wrapper from Cognitect:
- does not support x-ray
- does not support pre-signed S3 requests
- is alpha

## Why local server?
- running the server from a REPL provides access to handler from REPL (try scope capture in tests or local server)
- don't need SAM or SAM CLI
