# Prerequisites
**HL requires the following tools to be available**
  - [aws](https://aws.amazon.com/cli/)
  - [aws-sam](https://aws.amazon.com/serverless/sam/) - only for getting started guide
  - [docker](https://docs.docker.com/engine/install/)
  - [babashka](https://github.com/babashka/babashka)
  - [clojure](https://clojure.org/guides/getting_started)

## Quick install
  > :information_source: Docker should be installed separately 
  
  > :information_source: Provided installation method requires [brew](https://brew.sh/) 
  
  ```bash
  brew tap aws/tap && \
  brew install clojure/tools/clojure borkdude/brew/babashka awscli aws-sam-cli 
  ```
  
## AWS programmatic access credentials
  1. [Create an AWS account](https://portal.aws.amazon.com/billing/signup)
  2. Log in to your newly created account.
  3. When in AWS Management Console. Click on your name on the top right.
  4. Choose **My Security Credentials** sublink.
  5. Clink on users, then select your username. 
  6. On user summary page click on **Security credentials**.
  7. Under **Access keys** click **Create access key**
  8. Copy both **Access key ID** & **Secret access key**

## AWS CLI configuration
  This part is necessary for HL to operate, since HL requires valid AWS profile to be set up.
  
  > :information_source: You can change the AWS profile that HL uses via `HL_PROFILE` environment variable
  
  > :information_source: Alternatively you can set both `AWS_ACCESS_KEY_ID` & `AWS_SECRET_ACCESS_KEY` environment variables, then switch `HL_NO_PROFILE` environment variable to `true|1`

  1. Run the following command
    ```
    aws configure
    ```
  2. On `AWS Access Key ID` field paste the copied **Access key ID** from the previous section.
  3. On `AWS Secret Access Key` field paste the copied **Secret access key** from the previous section.
  4. On `Default region name` field type any of the available [AWS region](https://www.google.com/search?channel=fs&client=ubuntu&q=aws+region) to which the lambdas should be deployed.
  5. On `Default output format` you can just press enter.
  6. That's it. You can now start your journey with HL. 
