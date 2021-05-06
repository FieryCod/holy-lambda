# Getting started

## Dependencies
  You will need following things which you have to install on your own depending on your system.

  - Homebrew (for Mac OS) /Linuxbrew (for Linux)
  - Java 8
  - Docker, Docker Compose >= 1.13.1, 1.22.0

  After having all mentioned above dependencies just use the following commands:

  1. Install aws, aws-sam clojure and babashka>=0.3.7
     ```
     brew tap aws/tap && brew install awscli aws-sam-cli clojure/tools/clojure  borkdude/brew/babashka
     ```

  2. Configure default AWS profile via `aws-cli`. This is necessary for making a bucket and deploying an application. If you just want to test holy-lambda on local then this step is not necessary, but you will be able to use only a limited set of commands.

     ```
     aws configure
     ```

## First project 
1. Scaffold a project using:
  
   ```
   clojure -M:new -m clj-new.create holy-lambda basic.example 
   ```
   
   You should see following project structure when `cd` to the project directory:
   
   ```
    .
    ├── README.md
    ├── bb.edn
    ├── deps.edn
    ├── envs.json
    ├── resources
    │   └── native-agents-payloads
    │       └── 1.edn
    ├── src
    │   └── basic
    │       └── example
    │           └── core.cljc
    └── template.yml

    5 directories, 7 files
   ```
   
2. Try to sync dependencies in project:
   ```
   cd basic.example && bb stack:sync
   ```
   
   The first sync is not always successful. If you see a message of not successful sync, then remove the `.holy-lambda` folder and run `bb stack:sync` once again.
   The purpose of the `sync` command is to gather all dependencies from `bb.edn`, `deps.edn` for both Clojure, Native and Babashka runtime. Syncs checks as well whether any additional layers for runtime should be published. If `:self-manage-layers` flag is set to `false`, then `holy-lambda` will automatically publish all necessary layers and output `ARN` of each.
  
3. At this point you should have `.holy-lambda` directory in your project. If not then go to troubleshooting. Now you can choose one of three runtimes:

   - `:babashka`
   - `:native`
   - `:java`
   
   The template is adjusted in the way that all of the runtimes should work flawlessly and all you need to change is a `:runtime` value in `bb.edn`.
   
   1. `:babashka` runtime 
       Babashka runtime is probably the best one to start the journey with `holy-lambda`. 
       
       Run `bb stack:invoke`:
       ```
       ❯ bb stack:invoke
       [holy-lambda] Command <stack:invoke>
       Invoking basic.example.core.ExampleLambda (provided)
       arn:aws:lambda:eu-central-1:443526418261:layer:holy-lambda-babashka-runtime:20 is already cached. Skipping download
       Skip pulling image and use local one: samcli/lambda:provided-66f9fa34aefd240f454416a79.

       Mounting /home/fierycod/Workspace/Work/holy-lambda/examples/bb/babashka/basic.example/src as /var/task:ro,delegated inside runtime container
       START RequestId: 78eca6ee-0389-442c-ad81-9a0e3d64d3fb Version: $LATEST
       END RequestId: 78eca6ee-0389-442c-ad81-9a0e3d64d3fb
       REPORT RequestId: 78eca6ee-0389-442c-ad81-9a0e3d64d3fb  Init Duration: 0.04 ms  Duration: 198.68 ms     Billed Duration: 200 ms Memory Size: 2000 MB    Max Memory Used: 2000 MB
       {"statusCode":200,"headers":{"Content-Type":"text/plain; charset=utf-8"},"body":"Hello world. Babashka is sweet friend of mine! Babashka version: 0.3.7"}
       ```
    
       After some time you should see above output. First invocation is rather slow locally since `AWS SAM` has to download runtime image for babashka.
   


# Troubleshooting
  1. Running `bb stack:sync` results in:
     ```
     Project did not sync properly. Remove .holy-lambda directory and run
     ```
     
     *Solution*:
     ```
     bb stack:purge && bb stack:sync
     ```
   2. Commands are failing:
   
      *Solution*:
      ```
      bb stack:purge && bb stack:doctor
      ```
      
      Fix all errors reported by the tool. If you still experience any issue please report it at Github.
   3. GraalVM native-image compilation fails due to not enough RAM memory on MacOS
   
      *Solution**:
      Increate the RAM limit in Docker UI [preferences](https://docs.docker.com/docker-for-mac/#resources).
   
