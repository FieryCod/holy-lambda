import * as cdk from '@aws-cdk/core';
import * as apigateway from '@aws-cdk/aws-apigateway';
import * as lambda from '@aws-cdk/aws-lambda';
import * as s3 from '@aws-cdk/aws-s3';

export class InfraStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const bucket = new s3.Bucket(this, "hl-cdk-example");

    const handler = new lambda.Function(this, "HLCDKExampleLambda", {
      runtime: lambda.Runtime.PROVIDED,
      code: lambda.Code.fromAsset('../.holy-lambda/build/latest.zip'),
      handler: "cdk-example.core.ExampleLambda",
      environment: {
        BUCKET: bucket.bucketName
      }
    });

    bucket.grantReadWrite(handler);

    const api = new apigateway.RestApi(this, "hl-cdk-example-api", {
      restApiName: "Example CDK API",
      description: "Just says hello"
    });

    const SayHelloMethod = new apigateway.LambdaIntegration(handler);

    api.root.addMethod("GET", SayHelloMethod);
  }

}
