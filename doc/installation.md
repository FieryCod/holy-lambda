# Installation

## Prerequisites
You will need following things to start:
- Node.js >= v8.9.0
- Java >= 8
- Docker, Docker Compose >= 1.13.1, 1.22.0
- Python, Pip >= 3.5.0, 8.9.0

When you have all above installed then it's high time to install `fnative` utility.

1) *Install `fnative` in your path*

   Fnative is small utility which tries to compile `.jar` file using GraalVM in Docker environment.
   No matter whether you're using MacOS/Linux, because the `fnative` will output the binary which might be only executed within the Lambda Environment.

   ```
   wget https://raw.githubusercontent.com/FieryCod/holy-lambda/master/resources/fnative && chmod +x fnative && sudo mv fnative /usr/local/bin
   ```
