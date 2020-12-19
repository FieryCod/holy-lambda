# Prerequisites

- aws
- sam

## Additional prerequisites for building native lambdas

### Mac OS or Windows

- Docker

### Linux

- graalvm (including native-image) : run 'make install-graalvm' two levels up.

# Unoptimized, JVM version

```
make clean deploy
```

# Optimized, Graalified native version:

```
make clean native-deploy
```
