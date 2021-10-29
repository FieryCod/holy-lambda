# Troubleshooting 

## Project did not sync properly (fixed 0.6)
  Running `bb hl:sync` results in:
  
  ```bash
  [holy-lambda] Project did not sync properly. Remove .holy-lambda directory and run hl:sync
  ```

  **Solution**:
  ```bash
  bb hl:clean && bb hl:sync
  ```

## Some of the CLI commands are failing
  
   **Solution**:
   ```bash
   bb hl:clean && bb hl:doctor
   ```
      
  Fix all errors reported by the `hl:doctor`. If you still experience any issue please report it at [Github](https://github.com/FieryCod/holy-lambda/issues).
      
## Build request failed with exit status 137
  Error 137 is a common error code indicating not sufficient machine memory.
  If you're on the machine that have < 6GB of RAM you probably have to upgrade.
  
  **Error**:
  ```bash
  Error: Image build request failed with exit status 137
  com.oracle.svm.driver.NativeImage$NativeImageError: Image build request failed with exit status 137
    at com.oracle.svm.driver.NativeImage.showError(NativeImage.java:1772)
    at com.oracle.svm.driver.NativeImage.build(NativeImage.java:1519)
    at com.oracle.svm.driver.NativeImage.performBuild(NativeImage.java:1480)
    at com.oracle.svm.driver.NativeImage.main(NativeImage.java:1467)
  ```

  **Solution**: (MacOS)
  Docker Desktop on MacOS defaults to 2GB. You have to increase the RAM limit in Docker UI [preferences](https://docs.docker.com/docker-for-mac/#resources).
      
## Runtime.InvalidEntrypoint

   ```bash
   Mounting /path-to-source/holy-lambda-example/src as /var/task:ro,delegated inside runtime container
   START RequestId: 4dc6fcf5-7db2-4854-bba7-abae8038ef8f 
    Version: $LATEST 
      time="2021-05-17T16:52:48.278" level=error 
      msg="Init failed" InvokeID=xxx 
      error="Couldn't find valid bootstrap(s): [/var/task/bootstrap /opt/bootstrap /var/runtime/bootstrap]"
   time="2021-05-17T16:52:48.279" level=error msg="INIT DONE failed: Runtime.InvalidEntrypoint"
   ```
  
  This is most likely due to misconfiguration in either layers or `HL_ENTRYPOINT` environment variable.
  
  **Solution**:
  If you're using `babashka` runtime make sure to include corresponding layer.
  Set `HL_ENTRYPOINT` variable to your core namespace.

  ```yml
  Resources:
    ExampleFunction:
      Type: AWS::Serverless::Function
      Properties:
        Handler: com.example.core
        FunctionName: ExampleFunction
        Events:
          ProxyMessageEvent:
            Type: Api
            Properties:
              Path: /{message}
              Method: get
        Layers:
         - <ARN_OF_THE_DEPLOYED_BABASHKA_LAYER>
        Environment:
          Variables:
            HL_ENTRYPOINT: com.example
  
  ```
## Unable to use `:local/root` (deps.edn) (no longer required in 0.6.0)
  **Solution**:

  Holy lambda uses docker context for reproducible builds and GraalVM native-image compilation, therefore local libraries referenced in `deps.edn` will not work out of the box. However it's fairly simple to support local libraries via `:docker:volumes` + custom clojure alias.


  **Example**

  Let's assume the following project structure

  ```bash
  .
  ├── deps.edn
  ├── modules
  │   ├── holy-lambda-babashka-tasks
  │       ├── bb.edn
  │       ├── deps.edn
  │       ├── envs.json
  │       ├── src
  │       │   ├── example
  │       │   │   └── core.cljc
  │       │   └── holy_lambda
  │       │       └── tasks.clj
  │       └── template.yml
  ├── project.clj
  ├── src
  │   └── fierycod
  │       └── holy_lambda
  │           ├── agent.clj
  │           ├── core.cljc
  │           ├── custom_runtime.clj
  │           ├── response.clj
  │           └── util.cljc
  ```

  where:

  - `modules/holy-lambda-babashka-tasks` - is a project which that reference local library `holy-lambda` from the root path. 

    > :information_source: This module is both testing environment for `bb tasks` and the holy-lambda tasks source which is distributed via `:git/url` + `:sha` to end user projects.

  - `holy-lambda` - Project that provides the custom runtime. 

    > :information_source: We want to test new features of the runtime in special tasks environment, before we ship the new release of the runtime to the end users.

    In order to let `holy-lambda-babashka-tasks` use local `holy-lambda` library we have to change it's `bb.edn`:

    1. Navigate to `:holy-lambda/options:docker:volumes`
    2. Reference local library relative path and mount the local library in docker 

      ```clojure bb.edn
      {:holy-lambda/options 
        {:docker {:volumes [{:docker     "/holy-lambda"
                            :host       "../../"}]}
        ...
        }
        ...
      }
      ```
  3. We can check if volume has been succesfully mounted by running:
    ```clojure
    bb hl:docker:run:run "ls -la /holy-lambda"
    ```

  4. As a last step we have to use create and a reference a special alias in `bb.edn` that uses declared above `docker` mount paths.

    ```clojure modules/holy-lambda-babashka-tasks/deps.edn
    ;; As you can see the root path of the local library corresponds to the :docker mount directory.
    :aliases {:holy-lambda
              {:replace-deps {org.clojure/clojure {:mvn/version "1.10.3"}
                            io.github.FieryCod/holy-lambda {:local/root "/holy-lambda"}}}}
    ```

    ```clojure modules/holy-lambda-babashka-tasks/bb.edn
    :holy-lambda/options {...

                        :build {:clj-alias :holy-lambda} ;; <-- reference alias from deps.edn
                        ...
                        }
    ```

  Now your local library should work well with `holy-lambda`! :)

## Fatal oracle$VMError$HostedError
   
  Error message after using `bb hl:native:executable`

  ```bash
  error:com.oracle.svm.core.util.VMError$HostedError: SomeClassDefinition has no code offset set
  ```

  From my experience this issue occurs when GraalVM is unable to find the definition of the class or when the class on runtime mismatches the compiled one class.

  **Common scenarios in which the error occurs are**
  - using eval with quote instead of using `defmacro`
  - using local library which is packed by `lein uberjar` (bb hl:compile instead or depstar)
  - full aot compilation of the whole library (only main class should be aot compiled)
  - code which tries to `produce` the class on both compilation and run phase

## HL CLI hangs on M1 
  Use ARM64 version of holy-lambda-builder and switch to ARM64 architecture.  
