# Getting started

## Dependencies
  You will need following things which you have to install on your own depending on your system.

  - Homebrew (for Mac OS) /Linuxbrew (for Linux)
  - Java 8
  - Docker, Docker Compose >= 1.13.1, 1.22.0

  After having all mentioned above dependencies just use the following commands:

  1. Install make, aws, aws-sam, leiningen and clojure
    ```
    brew tap aws/tap && brew install make leiningen awscli aws-sam-cli clojure/tools/clojure
    ```

  2. Configure default AWS profile via `aws-cli`. This is necessary for making a bucket and deploying an application. If you just want to test holy-lambda on local then this step is not necessary, but you will be able to use only a limited set of commands.

   ```
   aws configure
   ```

## First project 

### A local, Java-runtime lambda

This will run a AWS Lambda API server with the Java runtime.

1. Run `lein new holy-lambda getting_started`.
2. `cd getting_started`.
3. Run `make dry-api`.

After some downloading of some artifacts, you should see:

```
sam local start-api --template template.yml
Mounting ExampleLambdaFunction at http://127.0.0.1:3000/ [GET]
You can now browse to the above endpoints to invoke your functions. You do not need to restart/reload SAM CLI while working on your functions, changes will be reflected instantly/automatically. You only need to restart SAM CLI if you update your AWS SAM template
2021-04-23 00:58:55  * Running on http://127.0.0.1:3000/ (Press CTRL+C to quit)
```

4. In another terminal, `cd` to this directory and run `make local-test`.

After a few seconds, in this second terminal, you should see the following output:

```
Connection to localhost port 3000 [tcp/hbci] succeeded!
curl "http://127.0.0.1:3000/"
Hello world%
```

5. Look again at the first terminal that's running the AWS Lambda server, you should see evidence of this api activity with something like:

```
START RequestId: 0b1999de-1a62-4cde-ab8f-bc536da8db7a Version: $LATEST
END RequestId: 0b1999de-1a62-4cde-ab8f-bc536da8db7a
REPORT RequestId: 0b1999de-1a62-4cde-ab8f-bc536da8db7a	Init Duration: 0.30 ms	Duration: 16856.78 ms	Billed Duration: 16900 ms	Memory Size: 2000 MB	Max Memory Used: 2000 MB
2021-04-23 01:01:46 127.0.0.1 - - [23/Apr/2021 01:01:46] "GET / HTTP/1.1" 200 -
```

You'll see these similar three lines every time a client accesses your API.

Hit Control-C to quit the AWS Lambda server.


### How did this work?

Running `make dry-api` started the `sam local start-api` with the `template.yml` template file as the input. The key parts of the `template.yml` file are:

- `CodeUri:` This specifies the relative path to the Uberjar `output.jar`.
- `Handler:` This is the name of a class in the `CodeUri` 's specified
class, contained in `output.jar`.

The Clojure source code for this
function is in `src/core/ExampleLambda.cljc`.

`sam local start-api` listens on port 3000 and waits for a client
connection. To handle the client connection, it starts a Docker
container using the image `amazon/aws-sam-cli-emulation-image-java8`,
running `/var/rapid/aws-lambda-rie` using your JAR and passing the
client's request parameters, runs your function within a JVM, and returns the
result to the client. It then terminates the Docker container after the client connection is closed.

### A local, native-runtime lambda

1. `cd` again to the `getting_started` directory you made in your first step.
2. Run `make dry-api-native`.

After some compilation, you should see:

```
sam local start-api --template template-native.yml --debug
...
2021-04-24 12:35:00,936 | 1 APIs found in the template
2021-04-24 12:35:00,949 | Mounting ExampleLambdaFunction at http://127.0.0.1:3000/ [GET]
2021-04-24 12:35:00,949 | You can now browse to the above endpoints to invoke your functions. You do not need to restart/reload SAM CLI while working on your functions, changes will be reflected instantly/automatically. You only need to restart SAM CLI if you update your AWS SAM template
2021-04-24 12:35:00,949 | Localhost server is starting up. Multi-threading = True
2021-04-24 12:35:00  * Running on http://127.0.0.1:3000/ (Press CTRL+C to quit)
```

3. As with the Java runtime, in another terminal, `cd` to this directory and run `make local-test` to call the API you've created from the Lambda server:

After a few seconds, in this second terminal, you should again see the following output:

```
Connection to localhost port 3000 [tcp/hbci] succeeded!
curl "http://127.0.0.1:3000/"
Hello world%
```

5. Look again at the first terminal that's running the AWS Lambda server, you should see evidence of this api activity with something like:

```
2021-04-24 12:38:29 127.0.0.1 - - [24/Apr/2021 12:38:29] "GET / HTTP/1.1" 200 -
```

The first time you call the API with `make local-test`, there will be
a delay as the Lambda server decompresses your compiled code from the
ZIP file. Subsequent calls will be faster, and should be noticeably
faster than the Java-runtime Lambda server.


