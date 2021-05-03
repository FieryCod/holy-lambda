# holy-lambda-template
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)
[![Clojars Project](https://img.shields.io/clojars/v/holy-lambda/lein-template.svg)](https://clojars.org/holy-lambda/lein-template)
---

Glorious template for Holy Lambda micro framework.
Visit the docs [here](https://cljdoc.org/d/io.github.FieryCod/holy-lambda/CURRENT))

## How to generate a project

``` 
lein new holy-lambda example && cd example && bb stack:sync
```

or 

``` clojure
clojure -M:new -m clj-new.create holy-lambda basic.example && cd basic.example && bb stack:sync
```

## License
Copyright © 2021 Karol Wojcik aka Fierycod

Released under the MIT license.
