# Getting started

This guide will quickly take you through the basics of using holy-lambda:

- Development environment setup
- Generate a scaffold project for your code
- Locally test the code in Docker 
- Deploy to AWS
- Invoke from API Gateway (optional)

Here's an overview ([version with working links](https://swimlanes.io/d/F_CZgZSY3)):

![alt text](https://static.swimlanes.io/717653ba1f693067e413ec5406c893f9.png "Overview")


## Prerequisites
  1. You will need an AWS account with [sufficient privileges](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/sam-permissions.html)

  2. The following components installed on your system:

      - [Homebrew](https://brew.sh) (for Mac OS) / [Linuxbrew](https://docs.brew.sh/Homebrew-on-Linux) (for Linux)
      - Java 8
      - Docker, Docker Compose >= 1.13.1, 1.22.0


## Dependencies

  1. Install aws, aws-sam, make, clojure, babashka (>= 0.4.1), and clj-kondo
     ```
     brew tap aws/tap && \
        brew install awscli \
                     aws-sam-cli \
                     make \
                     clojure/tools/clojure \
                     borkdude/brew/babashka \
                     borkdude/brew/clj-kondo
     ```
     
  2. Install [clj-new](https://github.com/seancorfield/clj-new) using these [instructions](https://github.com/seancorfield/clj-new#getting-started)

  3. Configure a **default** AWS profile via `aws-cli`. 
     This is necessary for interacting with AWS from holy-lambda.

     ```
     aws configure
     ```

## First project

1. Generate a scaffold project using the following:

   ```
   clojure -X:new :template holy-lambda :name com.company/example-lambda :output holy-lambda-example
   ```
   
   You should see following project structure when `cd` to the project directory:
   
   ```
   cd holy-lambda-example
   tree
    .
    ├── README.md
    ├── bb.edn
    ├── deps.edn
    ├── envs.json
    ├── resources
    │   └── native-agents-payloads
    │       └── 1.edn
    ├── src
    │   └── com
    │       └── company
    │           └── example_lambda
    │               └── core.cljc
    └── template.yml
    
    6 directories, 7 files
   ```
   
2. Before we begin, let's run some quick checks:

Some resource names in AWS, such as S3 buckets, are global and need to be unique. The scaffold generate you a unique bucket name, so your output will differ slightly to that below.

We can check that your AWS profile is working by simply creating and immediately removing the bucket:

```bash
bb bucket:create
[holy-lambda] Command <bucket:create>
[holy-lambda] Creating a bucket holy-lambda-example-5f3d731137724176b606beb6623b6f04
[holy-lambda] Bucket holy-lambda-example-5f3d731137724176b606beb6623b6f04 has been succesfully created!
```
```bash
bb bucket:remove
[holy-lambda] Command <bucket:remove>
[holy-lambda] Removing a bucket holy-lambda-example-5f3d731137724176b606beb6623b6f04
remove_bucket: holy-lambda-example-5f3d731137724176b606beb6623b6f04

```
    
3. sync the project dependencies:

The purpose of the `sync` command is to gather all dependencies from `bb.edn`, `deps.edn` for both Clojure, Native and Babashka runtimes. By default, sync also checks whether any additional Docker layers for runtime should be published (see `:self-manage-layers?` elsewhere**).

   > :warning:  Ensure docker is running at this point

   ```
   cd holy-lambda-example && bb stack:sync
   ```
   
   The first sync is not always successful. If this is the case check the following:  ** link to troubleshooting
- Is Docker running?
- Run `bb stack:purge` and run `bb stack:sync` once again
- If this still fails, run `bb stack:doctor` for diagnostic information
   
**This behaviour may be overridden by changing `:self-manage-layers?` flag is set to `false`, then `holy-lambda` will automatically publish all necessary layers and output `ARN` of each.
  
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
      Increase the RAM limit in Docker UI [preferences](https://docs.docker.com/docker-for-mac/#resources).
   
