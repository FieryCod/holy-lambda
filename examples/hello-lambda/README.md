# Prerequisites

- aws
- sam

## Additional prerequisites for building native lambdas

### Mac OS or Windows

- Docker

### Linux

- graalvm (including `native-image`) : run 'make install-graalvm' two levels up.


# Deploy a locally-hosted Lambda

### Linux

You have the option of:

- `make dry-api`

This runs the java artifact `output.jar` using `sam local`.

- `make native-dry-api`

This compiles the Clojure code into a `.jar`, and then runs GraalVM
(natively, on your workstation) to create a Linux x86 binary from this
`.jar`, and then zips this binary along with a short bootstrap shell
script. Then, `sam local` is started, which creates a Docker container running
Amazon's Lambda hosting environment with `./resources/native-template.yml` as its AWS SAM template file. This 
a Docker container that runs this template file references the zip file made in the first step.

### Mac OS or Windows

- `make native-dry-api`

This compiles the Clojure code into a `.jar`, and then runs GraalVM
within a Docker container to create a Linux x86 binary from this
`.jar`, and then zips this binary along with a short bootstrap shell
script. Then, `sam local` is started, which creates a Docker container running
Amazon's Lambda hosting environment with `./resources/native-template.yml` as its AWS SAM template file. This 
a Docker container that runs this template file references the zip file made in the first step.

- `make dry-api` 

This doesn't work for unknown reasons: the `sam local` log shows:

```
Mounting /private/var/folders/wn/t2vkdzx97nx15c11j4dgrky00000gn/T/tmpqfyd44p_ as /var/task:ro,delegated inside runtime container
START RequestId: ad39cf70-a7e5-4302-95b5-88162021a98b Version: $LATEST
time="2020-12-21T22:59:19.311" level=error msg="Init failed" InvokeID= error="fork/exec /var/task/bootstrap: permission denied"
time="2020-12-21T22:59:19.311" level=error msg="INIT DONE failed: Runtime.InvalidEntrypoint"
```
# Deploy an unoptimized, JVM version to AWS

```
make deploy
```

# Deploy on optimized, Graalified native version to AWS

```
make destroy native-deploy
```

Before transitioning from the JVM deployment to a native deployment,
or vice-versa, you must run `make destroy` (or `make clean`, which
runs `make destroy`), otherwise your Lambda will return a Server Error
due to a mismatch between the Lambda Application configuration and the
binary that is invoked by the Application's functions.
