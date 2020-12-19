# Prerequisites

- aws
- sam

## Additional prerequisites for building native lambdas:

- Docker

# Unoptimized, JVM version

```
make clean deploy
```

# Optimized, Graalified native version:

```
make clean native-deploy
```
