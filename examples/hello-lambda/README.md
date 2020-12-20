# Prerequisites

- aws
- sam

## Additional prerequisites for building native lambdas

### Mac OS or Windows

- Docker

### Linux

- graalvm (including `native-image`) : run 'make install-graalvm' two levels up.

# Unoptimized, JVM version

```
make destroy deploy
```

# Optimized, Graalified native version:

```
make destroy native-deploy
```

Before transitioning from the JVM deployment to a native deployment,
or vice-versa, you must run `make destroy` (or `make clean`, which
runs `make destroy`), otherwise your Lambda will return a Server Error
due to a mismatch between the Lambda Application configuration and the
binary that is invoked by the Application's functions.
