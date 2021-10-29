# Native backend
This section will take you through the basics of Native backend. 

**You will**
- Generate a scaffold project for your code
- Locally test the code 
- Deploy the project to AWS

> :information_source: Full project source can be find [here](https://github.com/FieryCod/holy-lambda/tree/master/docs/examples/getting-started/native-backend/holy-lambda-example)

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
3. Add following dependencies to the `deps.edn`
  ```
  com.github.clj-easy/graal-build-time {:mvn/version "0.1.4"}
  ```
  
  The library should populate `--initialize-at-built-time` argument with all the necessary Clojure namespaces. You can read about it [here](https://github.com/clj-easy/graal-build-time).
  
5. Compile
  ```bash
  bb hl:compile
  ```
  
6. Generate a native executable
  ```bash
  bb hl:native:executable
  ```
7. Try to invoke the function using `AWS SAM CLI`

  ```bash
  sam local invoke ExampleLambdaFunction
  ```

  > :information_source: The first invocation is rather slow locally since AWS SAM has to download runtime image for babashka. Subsequent invocations are much faster.

    **After some time you should see the following output**
    ```bash
    Invoking com.company.example-lambda.core.ExampleLambda (provided)
    Decompressing /home/fierycod/Workspace/Personal/Clojure/holy-lambda/docs/examples/getting-started/native-backend/holy-lambda-example/.holy-lambda/build/latest.zip
    Skip pulling image and use local one: public.ecr.aws/sam/emulation-provided:rapid-1.31.0.

    Mounting /tmp/tmp86nnqcb7 as /var/task:ro,delegated inside runtime container
    START RequestId: 9a7c74a8-21f6-42a2-8c40-f104a6d470f3 Version: $LATEST
    {"statusCode":200,"headers":{"content-type":"text/plain; charset=utf-8"},"body":"Hello world"}END RequestId: 9a7c74a8-21f6-42a2-8c40-f104a6d470f3
    REPORT RequestId: 9a7c74a8-21f6-42a2-8c40-f104a6d470f3 Init Duration: 0.44 ms Duration: 22.20 ms Billed Duration: 100 ms Memory Size: 128 MB Max Memory Used: 128 MB
    ```

    > :information_source: Highly recommend to check the official AWS SAM docs and play with other commands e.g. `sam local start-api`

#### Deployment
  Having successfully run the Lambda locally, we can now deploy to AWS. Application code will be uploaded to S3 as a `zip`.

  **Run the following command:**
  ```
  sam deploy --guided
  ```

**Default settings should be sufficient. Press enter for parameters and answer the questions as provided below**

```bash
Configuring SAM deploy
======================

        Looking for config file [samconfig.toml] :  Not found

        Setting default arguments for 'sam deploy'
        =========================================
        Stack Name [sam-app]: 
        AWS Region [eu-central-1]: 
        Parameter Timeout [40]: 
        Parameter MemorySize [128]: 
        Parameter Entrypoint [com.company.example-lambda.core]: 
        #Shows you resources changes to be deployed and require a 'Y' to initiate deploy
        Confirm changes before deploy [y/N]: y
        #SAM needs permission to be able to create roles to connect to the resources in your template
        Allow SAM CLI IAM role creation [Y/n]: y
        ExampleLambdaFunction may not have authorization defined, Is this okay? [y/N]: y
        Save arguments to configuration file [Y/n]: y
        SAM configuration file [samconfig.toml]: 
        SAM configuration environment [default]: 

        Looking for resources needed for deployment:
        Managed S3 bucket: <S3_BUCKET>
        A different default S3 bucket can be set in samconfig.toml

        Saved arguments to config file
        Running 'sam deploy' for future deployments will use the parameters saved above.
        The above parameters can be changed by modifying samconfig.toml
        Learn more about samconfig.toml syntax at 
        https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-config.html

        Uploading to sam-app/<OBJECT_KEY>  9073191 / 9073191  (100.00%)

        Deploying with following values
        ===============================
        Stack name                   : sam-app
        Region                       : eu-central-1
        Confirm changeset            : True
        Deployment s3 bucket         : <S3_BUCKET>
        Capabilities                 : ["CAPABILITY_IAM"]
        Parameter overrides          : {"Timeout": "40", "MemorySize": "128", "Entrypoint": "com.company.example-lambda.core"}
        Signing Profiles             : {}

Initiating deployment
=====================
Uploading to sam-app/<OBJECT_KEY>.template  1321 / 1321  (100.00%)

Waiting for changeset to be created..

CloudFormation stack changeset
---------------------------------------------------------------------------------------------------------------------------------------------------------
Operation                              LogicalResourceId                      ResourceType                           Replacement                          
---------------------------------------------------------------------------------------------------------------------------------------------------------
+ Add                                  ExampleLambdaFunctionHelloEventPermi   AWS::Lambda::Permission                N/A                                  
                                       ssion                                                                                                              
+ Add                                  ExampleLambdaFunctionRole              AWS::IAM::Role                         N/A                                  
+ Add                                  ExampleLambdaFunction                  AWS::Lambda::Function                  N/A                                  
+ Add                                  ServerlessHttpApiApiGatewayDefaultSt   AWS::ApiGatewayV2::Stage               N/A                                  
                                       age                                                                                                                
+ Add                                  ServerlessHttpApi                      AWS::ApiGatewayV2::Api                 N/A                                  
---------------------------------------------------------------------------------------------------------------------------------------------------------

Changeset created successfully. <ARN>


Previewing CloudFormation changeset before deployment
======================================================
Deploy this changeset? [y/N]: y
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

We created a `holy-lambda` project based on the Native backend and deployed it to `AWS Lambda`. 
 
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
- Deleting S3 object with key sam-app/408a4e133d83dbe223b5fe9322c58f7c
- Deleting S3 object with key sam-app/bf87f9eb6f91574945d3bb1bb473567d.template
- Deleting S3 object with key sam-app/e328fd57ea8768066cce90a5225df86e.template
- Deleting S3 object with key sam-app/fd4a54bbd77c97428e6ebbaaa12d2a69.template
- Deleting Cloudformation stack sam-app

Deleted successfully
```



