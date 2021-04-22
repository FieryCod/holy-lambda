# Getting started

## Dependencies
  You will need following things which you have to install on your own depending on your system.

  - Homebrew/Linuxbrew
  - Java 8
  - Docker, Docker Compose >= 1.13.1, 1.22.0

  After having all mentioned above dependencies just use the following commands:

  1. Install make, aws, aws-sam, leiningen and clojure
    ```
    brew tap aws/tap && brew install make leiningen awscli aws-sam-cli clojure/tools/clojure
    ```

  2. Configure default AWS profile via `aws-cli`. This is necessary for making a bucket and deploying an application. If you just want to test holy-lambda on local then this step is not necessary, but you will be able to use limited set of commands.

   ```
   aws configure
   ```

## First project 

### `dry-api`

This will run a AWS Lambda API server with the Java runtime.

1. In your local clone of `holy-lambda` directory, run `lein new holy-lambda getting_started`.
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

