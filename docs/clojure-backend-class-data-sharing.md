# Class Data Sharing

Java has a feature called Class Data Sharing (CDS) which can reduce the start-up of the Clojure backend runtime by up to 50%.

It does not require modification to your code application and is complementary to the limiting the tiered compilation if start-up performance is your goal.

## Approach

The build process for HolyLambda Clojure runtime uses a package type of `Image` using docker. We can extend the docker build to use CDS to reduce Java start up time.

Note that the CDS procedure varies according to the JVM version. We're demonstrating using JDK 19 since that is currently the simplest method. See [Suggested improvements](#suggested-improvements) for other JDKs.

The basic CDS process is as follows:
* create a JSON payload representing an event to send to the Lambda function
* recreate necessary environment variables at build time
* invoke the Lambda function during a docker build to touch on as many of its classes as possible
* capture the CDS output (as a file called `dynamic-cds.jsa`)
* package CDS output with the docker image
* use CDS (`dynamic-cds.jsa`) in the `java` launch command

The Lambda invocation process calls back out to an AWS endpoint to retrieve the event payload. To mock the AWS endpoint and serve our JSON event payload, we will use `jwebserver` which is now part of the JDK.

## Prerequisites

* A Clojure backend Lambda.

## Steps

### Event payload

From the root of your project, run the following:
```shell
mkdir -p mock-lambda/2018-06-01/runtime/invocation

# the target path and filename must match the following 
cp event.json mock-lambda/2018-06-01/runtime/invocation/next
```

Example content:

```json
{
  "Records" : [ {
    "body" : "content of an SQS message"
  } ]
}
```

Tip: If you have event payloads from a native build, you can use [jet](https://github.com/borkdude/jet) to extract/convert the payload:

```shell
cat resources/native-agents-payloads/1.edn | jet -f '#(get-in % [:request :event])' -o json > mock-lambda/2018-06-01/runtime/invocation/next
```

### Build time environment variables

The Lambda will use environment variables as part of its configuration either **explicitly** as part of your app's config, or **implicitly** as part of the Lambda execution (for example, to access AWS resources `AWS_ACCESS_ID_KEY`, `AWS_REGION` etc. need to be set)

For example:

```yaml
Resources:
  MyLambda:
    Type: AWS::Serverless::Function
    Properties:
      # ...

      Environment:
        Variables:
          MY_ENV_VARIABLE: "my value"
```

At the time of writing, `sam local`, `sam build`, and `sam deploy` each have [mutually incompatible ways](https://github.com/awslabs/aws-sam-cli/issues/1163#issuecomment-513500473) to specify environment variable values.

We're concerned with `sam build`, specifically for an **image type**. Please note that the docs for `sam build` refer to `--container-env-var` for "setting env variables" [but this is for Zip package type only](https://github.com/aws/aws-sam-cli/issues/2972)

The approach below navigates all of this, allowing us to specify environment variables at build time.

Amend your application's SAM template (`template.yml`) - note the command line overridable parameters in the `Parameters` section. The rest of the env parameters are set in the `Resources.MyLambda.Metadata` section.

```yaml
AWSTemplateFormatVersion: '2010-09-09'
Transform: AWS::Serverless-2016-10-31
Description: >
  Demonstrate CDS

Parameters:
  # ...

  # BUILD-TIME ONLY PARAMETERS (only parameters supplied via the command line need to be declared here)
  # For secrets or dynamic content, use the command line to provide values:
  # sam build --parameter-overrides "BuildAwsAccessKeyId=\"$(aws configure get aws_access_key_id)\",BuildAwsSecretAccessKey=\"$(aws configure get aws_secret_access_key)\""

  BuildAwsAccessKeyId:
    Type: String
    Description: The AWS_ACCESS_KEY_ID for build-time invocation
    Default: build-time only

  BuildAwsSecretAccessKey:
    Type: String
    Description: The AWS_SECRET_ACCESS_KEY for build-time invocation
    Default: build-time only

Globals:
  Function:
    Environment:
      Variables:
        HL_ENTRYPOINT: !Ref Entrypoint

Resources:
  MyLambda:
    Type: AWS::Serverless::Function
    Properties:
      FunctionName: "MyLambdaFunction"
      PackageType: Image
      # Memory allocation in Mb
      # * CPU is proportional to memory allocation; 1792 is where you get a whole CPU.
      MemorySize: 4096

      Events:
        # ...

      Environment:
        Variables:
          MY_ENV_VARIABLE: "my value"

    Metadata:
      Dockerfile: Dockerfile
      DockerContext: .
      DockerTag: v1
      DockerBuildArgs:
        # fully qualified package + path to the Lambda's entry point function
        MAIN_CLASS: example.MyLambda

        # Environment variables required by the Lambda function for build-time invocation.
        AWS_ACCESS_KEY_ID: !Ref BuildAwsAccessKeyId
        AWS_SECRET_ACCESS_KEY: !Ref BuildAwsSecretAccessKey
        AWS_REGION: us-east-1

        MY_ENV_VARIABLE: "another value used only for build"
```

Here's an example `Dockerfile` using CDS 

```dockerfile
# Need JDK 18+ for jwebserver
FROM amazoncorretto:19-al2-jdk

ADD .holy-lambda/build/output.jar output.jar

# Prerendered event payload to be served by jwebserver to emulate event delivery in the Lambda environment.
ADD mock-lambda/2018-06-01/runtime/invocation/next mock-lambda/2018-06-01/runtime/invocation/next

# Need both ARG (to receive value via build arg) and ENV (to make the variable available at runtime)
ARG MAIN_CLASS="${MAIN_CLASS}"
ENV MAIN_CLASS="${MAIN_CLASS}"

# Build args
ARG AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID}"
ARG AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY}"
ARG AWS_REGION="${AWS_REGION}"

# Environment variables required by the Lambda function for build-time invocation.
ARG MY_ENV_VARIABLE="${MY_ENV_VARIABLE}"

# Lambda execution environment variables. Leave as-as.

# Endpoint host where AWS to serve event payload
ARG AWS_LAMBDA_RUNTIME_API="localhost" 
ARG AWS_LAMBDA_FUNCTION_MEMORY_SIZE="1024"
ARG AWS_LAMBDA_FUNCTION_NAME="build-function"
ARG AWS_LAMBDA_FUNCTION_VERSION="\$LATEST"
ARG AWS_LAMBDA_LOG_GROUP_NAME="/aws/lambda/build-function"
ARG AWS_LAMBDA_LOG_STREAM_NAME="2022/11/03/[]92666dab180248259978ce251f50d5f7"

# set outputs all environment variables. Ensure that these are as you expect.
RUN set \
    # jwebserver was introduced in JDK 18.
    && (nohup $JAVA_HOME/bin/jwebserver -p 80 -d /mock-lambda &) \
    # need to wait for webserver above to be ready (2 should be enough, but using 5 to be sure)
    && sleep 5 \
    # run the Lambda to create enough of an execution profile for Class Data Sharing (CDS). The "exit 0" is necessary since the Lamdba will not exit cleanly due as it cannot POST the Lambda result to jwebserver (it only supports GET/HEAD requests)
    && (java -Xlog:cds -XX:ArchiveClassesAtExit=dynamic-cds.jsa -jar output.jar "${MAIN_CLASS}"; exit 0) \
    && ls -lh *.*

# Runtime command using the dynamic-cds.jsa file generated at build-time. Add "-Xlog:cds" to params for info on CDS
CMD java -XX:SharedArchiveFile=dynamic-cds.jsa -jar output.jar "${MAIN_CLASS}"
```

## Build

To build, use the following (remove parameter overrides if not using):

```shell
sam build --parameter-overrides "BuildAwsAccessKeyId=\"$(aws configure get aws_access_key_id)\",BuildAwsSecretAccessKey=\"$(aws configure get aws_secret_access_key)\""
```

Deploy as normal.


## Suggested improvements

The web service supplied with the JDK since 18 was used for convenience. You could supply an alternate micro webserver and use a [multi-stage docker build](https://docs.docker.com/build/building/multi-stage/) to prevent any additional build dependencies leaking into the final artefact.

From JDK 13 onwards, you can use ArchiveClassesAtExit as described above. With JDK versions 10+, [this article](https://nipafx.dev/java-application-class-data-sharing/) describes the process. 
