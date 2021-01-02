# Installation

You will need following things to start:
- Homebrew/Linuxbrew
- Java 11
- Docker, Docker Compose >= 1.13.1, 1.22.0

1. First install all dependencies:
    ```
    brew tap aws/tap && brew install leiningen awscli aws-sam-cli clojure/tools/clojure
    ```

2. Configure default AWS profile via `aws-cli`
   ```
   aws configure
   ```

That's it! Navigate to [next section](https://cljdoc.org/d/fierycod/holy-lambda/CURRENT/doc/new-project-generation)
