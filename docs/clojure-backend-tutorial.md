# Clojure backend
This section will take you through the basics of Clojure backend. 

**You will**
- Generate a scaffold project for your code
- Locally test the code 
- Deploy the project to AWS

> :information_source: Full project source can be find [here](https://github.com/FieryCod/holy-lambda/tree/master/docs/examples/getting-started/clojure-backend/holy-lambda-example)

1. We'll generate our first project using the `holy-lambda` project template. This will create a project tree with all the necessary resources to get us started.

  ```bash
  clojure -X:new :template holy-lambda :name com.company/example-lambda :output holy-lambda-example
  ```
2. Go into the the project directory

    ```bash
    cd holy-lambda-example
    ```

    You should see following project structure:

    ```bash
    $$ tree .
    .
    ├── bb.edn
    ├── deps.edn
    ├── Dockerfile
    ├── README.md
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
3. (Optional, but recommended) If you willing to use [tiered](https://aws.amazon.com/blogs/compute/increasing-performance-of-java-aws-lambda-functions-using-tiered-compilation/) compilation modify your `Dockerfile`, and add the following options before `-jar` argument.

   ```bash
   -XX:+TieredCompilation -XX:TieredStopAtLevel=1 
   ```
   **Example**
   ```Dockerfile
   CMD java -jar output.jar "com.company.example-lambda.core.ExampleLambda"
   ```
4. Modify `template.yml`
   1. Remove `CodeUri` from `Parameters`. Remove it from `Globals` as well.
   2. Remove `Runtime` entry from `Globals`. You can remove it from `Parameters` as well.
   3. Tune `MemorySize` parameter to `512`.
   4. Remove `Handler` from the function properties.
   5. Replace `ExampleLambdaFunction` block in the resources with following.
      ```yml
      ExampleLambdaFunction:
        Type: AWS::Serverless::Function
        Properties:
          FunctionName: ExampleLambdaFunction
          PackageType: Image
          Events:
            HelloEvent:
              Type: HttpApi
              Properties:
                ApiId: !Ref ServerlessHttpApi
                Path: /
                Method: GET
        Metadata:
          Dockerfile: Dockerfile
          DockerContext: .
          DockerTag: v1
      ```
      
      **This is how your template.yml should look like**
      ```yml
      AWSTemplateFormatVersion: '2010-09-09'
      Transform: AWS::Serverless-2016-10-31
      Description: >
        Example basic lambda using `holy-lambda` micro library

      Parameters:
        Timeout:
          Type: Number
          Default: 40
        MemorySize:
          Type: Number
          Default: 512
        Entrypoint:
          Type: String
          Default: com.company.example-lambda.core

      Globals:
        Function:
          Timeout: !Ref Timeout
          MemorySize: !Ref MemorySize
          Environment:
            Variables:
              HL_ENTRYPOINT: !Ref Entrypoint

      Resources:
        ExampleLambdaFunction:
          Type: AWS::Serverless::Function
          Properties:
            FunctionName: ExampleLambdaFunction
            PackageType: Image
            Events:
              HelloEvent:
                Type: HttpApi
                Properties:
                  ApiId: !Ref ServerlessHttpApi
                  Path: /
                  Method: GET
          Metadata:
            Dockerfile: Dockerfile
            DockerContext: .
            DockerTag: v1

        ServerlessHttpApi:
          Type: AWS::Serverless::HttpApi
          DeletionPolicy: Retain
          Properties:
            StageName: Prod

      Outputs:
        ExampleLambdaEndpoint:
          Description: Endpoint for ExampleLambdaFunction
          Value:
            Fn::Sub: https://${ServerlessHttpApi}.execute-api.${AWS::Region}.amazonaws.com

      ```
5. Compile
  ```
  bb hl:compile
  ```
  
6. Build a docker image using:
  ```bash
  sam build
  ```
  
    **Build should be successful at this point**
    ```bash
    Building codeuri: /home/fierycod/Workspace/Personal/Clojure/holy-lambda/docs/examples/getting-started/clojure-backend/holy-lambda-example runtime: Runtime metadata: {'Dockerfile': 'Dockerfile', 'DockerContext': '/home/fierycod/Workspace/Personal/Clojure/holy-lambda/docs/examples/getting-started/clojure-backend/holy-lambda-example', 'DockerTag': 'v1'} functions: ['ExampleLambdaFunction']
    Building image for ExampleLambdaFunction function
    Setting DockerBuildArgs: {} for ExampleLambdaFunction function
    Step 1/4 : FROM openjdk:latest
    ---> f4489eef8885
    Step 2/4 : MAINTAINER Karol Wójcik <karol.wojcik@tuta.io>
    ---> Using cache
    ---> 4501e45849c4
    Step 3/4 : ADD .holy-lambda/build/output.jar output.jar
    ---> Using cache
    ---> f8b49bf1a118
    Step 4/4 : CMD java -jar output.jar "com.company.example-lambda.core.ExampleLambda"
    ---> Using cache
    ---> a892a69f1800
    Successfully built a892a69f1800
    Successfully tagged examplelambdafunction:v1

    Build Succeeded

    Built Artifacts  : .aws-sam/build
    Built Template   : .aws-sam/build/template.yaml

    Commands you can use next
    =========================
    [*] Invoke Function: sam local invoke
    [*] Deploy: sam deploy --guided
    ```

7. Try to invoke the function using `AWS SAM CLI`

  ```bash
  sam local invoke ExampleLambdaFunction
  ```

  > :information_source: The first invocation is rather slow locally since AWS SAM has to download runtime image for babashka. Subsequent invocations are much faster.

    **After some time you should see the following output**
    ```bash
    Invoking Container created from examplelambdafunction:v1
    Skip pulling image and use local one: examplelambdafunction:rapid-1.31.0.

    START RequestId: fb6472ed-dc42-4654-b385-d8c7f3ad834f Version: $LATEST
    {"statusCode":200,"headers":{"content-type":"text/plain; charset=utf-8"},"body":"Hello world"}
    
    END RequestId: fb6472ed-dc42-4654-b385-d8c7f3ad834f
    REPORT RequestId: fb6472ed-dc42-4654-b385-d8c7f3ad834f Init Duration: 0.06 ms Duration: 767.78 ms Billed Duration: 800 ms Memory Size: 512 MB Max Memory Used: 512 MB
    ```

    > :information_source: Highly recommend to check the official AWS SAM docs and play with other commands e.g. `sam local start-api`

#### Deployment
Having successfully run the Lambda locally, we can now deploy to AWS. Since we're using `Image` as a `PackageType` the `sam deploy --guided` will create a managed `ECR registry` for us and push the new Docker image to the registry.

**Run the following command:**
```
sam deploy --guided
```

**Default settings should be sufficient. Press enter for parameters and answer the questions as provided below**

```bash
Configuring SAM deploy
======================

        Looking for config file [samconfig.toml] :  Found
        Reading default arguments  :  Success

        Setting default arguments for 'sam deploy'
        =========================================
        Stack Name [sam-app]: 
        AWS Region [eu-central-1]: 
        Parameter Timeout [40]: 
        Parameter MemorySize [512]: 
        Parameter Entrypoint [com.company.example-lambda.core]: 
        #Shows you resources changes to be deployed and require a 'Y' to initiate deploy
        Confirm changes before deploy [Y/n]: y
        #SAM needs permission to be able to create roles to connect to the resources in your template
        Allow SAM CLI IAM role creation [Y/n]: y
        ExampleLambdaFunction may not have authorization defined, Is this okay? [y/N]: y
        Save arguments to configuration file [Y/n]: y
        SAM configuration file [samconfig.toml]: 
        SAM configuration environment [default]: 

        Looking for resources needed for deployment:
        ExampleLambdaFunction: <CLIENT_ID>.dkr.ecr.eu-central-1.amazonaws.com/<MANAGED>

        Saved arguments to config file
        Running 'sam deploy' for future deployments will use the parameters saved above.
        The above parameters can be changed by modifying samconfig.toml
        Learn more about samconfig.toml syntax at 
        https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-config.html

        The push refers to repository [<CLIENT_ID>.dkr.ecr.eu-central-1.amazonaws.com/<MANAGED>]


        Deploying with following values
        ===============================
        Stack name                   : sam-app
        Region                       : eu-central-1
        Confirm changeset            : True
        Deployment image repository  : 
                                       {
                                           "ExampleLambdaFunction": "<ECR_IMAGE>"
                                       }
        Deployment s3 bucket         : <S3_BUCKET>
        Capabilities                 : ["CAPABILITY_IAM"]
        Parameter overrides          : {"Timeout": "40", "MemorySize": "512", "Entrypoint": "com.company.example-lambda.core"}
        Signing Profiles             : {}

Initiating deployment
=====================
Uploading to sam-app/915c620d2e4b41b6a5e7cffa11db018f.template  1377 / 1377  (100.00%)

Waiting for changeset to be created..

CloudFormation stack changeset
-------------------------------------------------------------------------------------------------
Operation                LogicalResourceId        ResourceType             Replacement            
-------------------------------------------------------------------------------------------------
+ Add                    ExampleLambdaFunctionH   AWS::Lambda::Permissio   N/A                    
                         elloEventPermission      n                                               
+ Add                    ExampleLambdaFunctionR   AWS::IAM::Role           N/A                    
                         ole                                                                      
+ Add                    ExampleLambdaFunction    AWS::Lambda::Function    N/A                    
+ Add                    ServerlessHttpApiApiGa   AWS::ApiGatewayV2::Sta   N/A                    
                         tewayDefaultStage        ge                                              
+ Add                    ServerlessHttpApi        AWS::ApiGatewayV2::Api   N/A                    
-------------------------------------------------------------------------------------------------

Changeset created successfully. <DEPLOYMENT_ARN>
```

Now `AWS SAM` will deploy the application!

```bash
Previewing CloudFormation changeset before deployment
======================================================
Deploy this changeset? [y/N]: y

2021-09-13 15:07:38 - Waiting for stack create/update to complete

CloudFormation events from changeset
-------------------------------------------------------------------------------------------------
ResourceStatus           ResourceType             LogicalResourceId        ResourceStatusReason   
-------------------------------------------------------------------------------------------------
CREATE_IN_PROGRESS       AWS::IAM::Role           ExampleLambdaFunctionR   -                      
                                                  ole                                             
CREATE_IN_PROGRESS       AWS::IAM::Role           ExampleLambdaFunctionR   Resource creation      
                                                  ole                      Initiated              
CREATE_COMPLETE          AWS::IAM::Role           ExampleLambdaFunctionR   -                      
                                                  ole                                             
CREATE_IN_PROGRESS       AWS::Lambda::Function    ExampleLambdaFunction    -                      
CREATE_IN_PROGRESS       AWS::Lambda::Function    ExampleLambdaFunction    Resource creation      
                                                                           Initiated              
CREATE_COMPLETE          AWS::Lambda::Function    ExampleLambdaFunction    -                      
CREATE_IN_PROGRESS       AWS::ApiGatewayV2::Api   ServerlessHttpApi        -                      
CREATE_COMPLETE          AWS::ApiGatewayV2::Api   ServerlessHttpApi        -                      
CREATE_IN_PROGRESS       AWS::ApiGatewayV2::Api   ServerlessHttpApi        Resource creation      
                                                                           Initiated              
CREATE_IN_PROGRESS       AWS::Lambda::Permissio   ExampleLambdaFunctionH   -                      
                         n                        elloEventPermission                             
CREATE_IN_PROGRESS       AWS::Lambda::Permissio   ExampleLambdaFunctionH   Resource creation      
                         n                        elloEventPermission      Initiated              
CREATE_IN_PROGRESS       AWS::ApiGatewayV2::Sta   ServerlessHttpApiApiGa   -                      
                         ge                       tewayDefaultStage                               
CREATE_COMPLETE          AWS::ApiGatewayV2::Sta   ServerlessHttpApiApiGa   -                      
                         ge                       tewayDefaultStage                               
CREATE_IN_PROGRESS       AWS::ApiGatewayV2::Sta   ServerlessHttpApiApiGa   Resource creation      
                         ge                       tewayDefaultStage        Initiated              
CREATE_COMPLETE          AWS::Lambda::Permissio   ExampleLambdaFunctionH   -                      
                         n                        elloEventPermission                             
CREATE_COMPLETE          AWS::CloudFormation::S   sam-app                  -                      
                         tack                                                                     
-------------------------------------------------------------------------------------------------

CloudFormation outputs from deployed stack
-------------------------------------------------------------------------------------------------
Outputs                                                                                         
-------------------------------------------------------------------------------------------------
Key                 ExampleLambdaEndpoint                                                       
Description         Endpoint for ExampleLambdaFunction                                          
Value               https://<ENDPOINT>                                                                            
-------------------------------------------------------------------------------------------------

Successfully created/updated stack - sam-app in eu-central-1
```

Congratulations. You can now check your API running by clicking on `Serverless HttpApi URL`, that is the output of `sam deploy --guided`.

#### Conclusion

In this guide, we've covered many of the basics with `holy-lambda`. We've covered quite a lot actually, so well done for getting this far!

We created a `holy-lambda` project based on the Clojure backend and deployed it to `AWS Lambda`. 
 
We hope you enjoy using Clojure in AWS Lambdas using `holy-lambda`

#### Clean up

The resources created in this guide incur minimal AWS costs when they're not being executed.

If you prefer to completely remove the resources using the following command to tear down and delete the application stack:

```bash
sam delete
```

**Output**
```
Are you sure you want to delete the stack sam-app in the region eu-central-1 ? [y/N]: y
Are you sure you want to delete the folder sam-app in S3 which contains the artifacts? [y/N]: y
Found ECR Companion Stack <STACK_NAME>
Do you you want to delete the ECR companion stack <STACK_NAME> in the region eu-central-1 ? [y/N]: y
ECR repository <STACK_NAME> may not be empty. Do you want to delete the repository and all the images in it ? [y/N]: y
- Deleting ECR repository <STACK_NAME_ECR_REPOSITORY>
- Deleting ECR Companion Stack sam-app-7427b055-CompanionStack
- Deleting S3 object with key <OBJECT_KEY>.template
- Deleting Cloudformation stack sam-app

Deleted successfully
```


