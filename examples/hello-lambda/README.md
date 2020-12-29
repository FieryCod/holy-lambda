# Prerequisites

- aws
- sam

## Additional prerequisites for building native lambdas

### Mac OS or Windows

- Docker

### Linux

- graalvm (including `native-image`) : run 'make install-graalvm' two levels up.


# Deploy a locally-hosted Lambda

You have the option of:

- `make dry-api`

This runs the java artifact `output.jar` using `sam local` with `template.yml` as the AWS SAM template file.

- `make dry-api-native`

This compiles the Clojure code into a `.jar`, and then runs GraalVM
(on Linux, this is done natively, on your workstation, whereas on non-Linux workstations, this is done via Docker) to create a Linux x86 binary from this
`.jar`, and then zips this binary along with a short bootstrap shell
script. Then, `sam local` is started, which creates a Docker container running
Amazon's Lambda hosting environment with `native-template.yml` as its AWS SAM template file.

# Deploy an unoptimized, JVM version to AWS

```
make destroy deploy
```

# Deploy on optimized, Graalified native version to AWS

```
make destroy deploy-native
```

Before transitioning from the JVM deployment to a native deployment,
or vice-versa, you must run `make destroy`, otherwise your Lambda will return a Server Error
due to a mismatch between the Lambda Application configuration and the
binary that is invoked by the Application's functions.
