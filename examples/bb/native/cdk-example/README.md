# cdk-example

This example demonstrates the usage of HL with CDK.

`/infra` directory was created via command: `cdk init app --language=typescript`

Additional modules has been installed via:

``` sh 
# in /infra
yarn add @aws-cdk/aws-apigateway @aws-cdk/aws-lambda @aws-cdk/aws-s3
```

Since I'm using assets I have to bootstrap the environment:

``` sh
# in /infra
cdk bootstrap
```

## Deployment
  1. `bb stack:sync && bb stack:compile && bb native:executable && cd infra && cdk deploy`
