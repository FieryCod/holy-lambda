# Getting started

## Dependencies
  You will need following things which you have to install on your own depending on your system.

  - Homebrew/Linuxbrew
  - Java 8
  - Docker, Docker Compose >= 1.13.1, 1.22.0

  After having all mentioned above dependencies just use the following commands:

  1. Install aws, aws-sam, leiningen and clojure
    ```
    brew tap aws/tap && brew install leiningen awscli aws-sam-cli clojure/tools/clojure
    ```

  2. Configure default AWS profile via `aws-cli`. This is necessary for making a bucket and deploying an application. If you just want to test holy-lambda on local then this step is not necessary, but you will be able to use limited set of commands.

   ```
   aws configure
   ```

## First project 
