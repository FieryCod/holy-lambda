# aws-interop-v2.example

An example of a Lambda that is deployed :native on AWS but runs in :java on localhost.

Slightly different from other Holy Lambda examples in that it's built to a `native:executable` locally or in CI and then
deployed to AWS using the AWS CLI i.e. not the HL bb tasks.

### Getting Started

Ensure Docker is running.

Download the deps required using `bb stack:sync`

Start a REPL (with dev and test aliases active) and run the test.

Then eval the form in user.clj to start a local server and navigate a browser to http://localhost:3001

For tests or local server, use scope capture in the handler to explore/iterate on data/features.

To build a native image suitable for upload to an AWS lambda:

1. find `yourbucketname` in the handler ns and change it to a bucket accessible to your AWS account
2. `bb stack:compile && bb native:conf :runtime native && bb native:executable :runtime native`

Note: without AWS creds in the environment, the agent/in-context calls will fail but should still record enough data for
a good executable. You can run the same commands locally with creds in CI with creds to improve this.

### Features

1. 2 different S3 calls made by Lambda to provide data
2. Uses the AWS Java API via interop instead of the aws-api from Cognitect
3. Uses AWS client fakes in tests and mocks for native:conf invocations
4. provides a ring local http server that calls the handler using mocks that read from the local file-system

## Why not aws-api?

The more idiomatic Clojure wrapper from Cognitect:

- does not support x-ray
- does not support pre-signed S3 requests

## Why local server?

- running the server from a REPL provides access to handler from REPL (try scope capture in tests or local server)
- don't need SAM or SAM CLI
