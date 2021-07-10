# {{name}}

## First steps
  1. Run `bb stack:sync` to download dependencies
  2. Run `bb stack:doctor` to validate if current state of application is fine
  
## Run

### Java runtime
``` sh
bb stack:compile && bb stack:invoke
```

### Native runtime

``` sh
bb stack:compile && bb native:executable && bb stack:invoke
```


### Babashka runtime

``` sh
bb stack:invoke
```

## Clean

``` sh
bb stack:prune
```

## Deploy application

``` sh
bb stack:pack && bb stack:deploy
```

## Destroy application

``` sh
bb stack:destroy
```

## Getting help
  - [HL Docs](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT)
  - [HL Slack](https://clojurians.slack.com/archives/C01UQJ4JC9Y)
  - [GraalVM Clojure Docs](https://github.com/lread/clj-graal-docs/blob/master/doc/testing-strategies.md)
  - [Clojure GraalVM Slack](https://clojurians.slack.com/archives/CAJN79WNT)
  - [Official GraalVM Channel](https://graalvm.slack.com/ssb/redirect)

## License

Copyright © 2021 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

