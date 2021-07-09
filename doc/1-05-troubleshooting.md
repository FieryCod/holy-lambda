# Troubleshooting
  1. Running `bb stack:sync` results in:
     ```
     [holy-lambda] Project did not sync properly. Remove .holy-lambda directory and run stack:sync
     ```
     
     **Solution**:
     ```bash
     bb stack:purge && bb stack:sync
     ```
  
  2. Commands are failing:
   
      **Solution**:
      ```bash
      bb stack:purge && bb stack:doctor
      ```
      
      Fix all errors reported by the tool. If you still experience any issue please report it at [Github](https://github.com/FieryCod/holy-lambda/issues).
      
  3. GraalVM native-image compilation fails due to not enough RAM memory on MacOS
      **Error**:
      ```
      Error: Image build request failed with exit status 137
      com.oracle.svm.driver.NativeImage$NativeImageError: Image build request failed with exit status 137
        at com.oracle.svm.driver.NativeImage.showError(NativeImage.java:1772)
        at com.oracle.svm.driver.NativeImage.build(NativeImage.java:1519)
        at com.oracle.svm.driver.NativeImage.performBuild(NativeImage.java:1480)
        at com.oracle.svm.driver.NativeImage.main(NativeImage.java:1467)
      ```
   
      **Solution**:
      
      Increase the RAM limit in Docker UI [preferences](https://docs.docker.com/docker-for-mac/#resources).
      
  4. Command `bb stack:invoke` fails with the following message
      ```
      Mounting /path-to-source/holy-lambda-example/src as /var/task:ro,delegated inside runtime container
      START RequestId: 4dc6fcf5-7db2-4854-bba7-abae8038ef8f Version: $LATEST
      time="2021-05-17T16:52:48.278" level=error msg="Init failed" InvokeID= error="Couldn't find valid bootstrap(s): [/var/task/bootstrap /opt/bootstrap   /var/runtime/bootstrap]"
      time="2021-05-17T16:52:48.279" level=error msg="INIT DONE failed: Runtime.InvalidEntrypoint"
      ```
  
      **Solution**:
      
      The layers of your template have not been configured correctly. Ensure that stack:sync reports an ARN and it has been added to the `template.yml`
      
  5. Could not find `/project/.holy-lambda/clojure-tools-1.10.3.849.jar`
  
     **Solution**:
     
     Ignore this. Nothing bad happens!
     
  6. Fatal `error:com.oracle.svm.core.util.VMError$HostedError: SomeClassDefinition has no code offset set`
  
     From my experience this issue occurs when GraalVM is unable to find the definition of the class or the class on runtime mismatches with the one which is compiled.
     
     Common scenarios in which the error occurs are:
     - using eval with quote instead of using `defmacro`
     - using local library which is packed by `lein uberjar` (use depstar instead)
     - full aot compilation of the whole library (only main class should be aot compiled)
     - code which tries to `produce` the class on both compilation and run phase
     
7. Unable to use local library with holy-lambda

   **Solution**:
   
   Holy lambda uses docker context for reproducible builds and GraalVM native-image compilation, therefore local libraries referenced in `deps.edn` will not work out of the box. However it's fairly simple to support local libraries via `:docker:volumes` + custom clojure alias.
   

   *Example*
   Let's assuming following project structure, where:
   
   `modules/holy-lambda-babashka-tasks` - Project which should reference local library `holy-lambda` from the root path. This module is both testing environment for `bb tasks` and the holy-lambda tasks source which is distributed via `:git/url` + `:sha` to end user projects.
   
   `holy-lambda` - Project which ships with custom runtime. We want to test new features of the runtime in special tasks environment, before we ship the new release of the runtime to the end users.
   
   ```
    .
    ├── CHANGELOG.md
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
    │           ├── interceptor.cljc
    │           ├── java_runtime.clj
    │           ├── native_runtime.clj
    │           ├── response.clj
    │           └── util.cljc
   ```
   
   In order to let `holy-lambda-babashka-tasks` use local `holy-lambda` library we have to change it's `bb.edn`:
   
   1. Navigate to `:holy-lambda/options:docker:volumes`
   2. Reference local library relative path and mount the local library to docker path
      
      ```clojure
      :holy-lambda/options {:docker {:volumes [{:docker     "/holy-lambda"
                                                :host       "../../"}]
                                      ...
                                      }
                            ...
                            }

      ```
  3. We can check if volume has been succesfully mounted by running:
     ```clojure
     bb docker:run "ls -la /holy-lambda"
     ```
  4. As a last step we have to use `:replace-deps` in `deps.edn` in some alias and reference the alias in `bb.edn:holy-lambda/options:build:clj-alias`, so that deps available in docker context could be added to the classpath.
  
     deps.edn in `holy-lambda-babashka-tasks`:
     ```clojure
      :aliases {:holy-lambda
                {:replace-deps {org.clojure/clojure {:mvn/version "1.10.3"}
                                io.github.FieryCod/holy-lambda {:local/root "/holy-lambda"}}}} ;; <-- as you can see the root path of the local library corresponds to the :docker mount directory.
     ```
     
     bb.edn in `holy-lambda-babashka-tasks`:
     
     ```clojure
      :holy-lambda/options {...

                            :build {:clj-alias :holy-lambda} ;; <-- reference alias from deps.edn
                            ...
                            }
     ```
     
   Now your local library should work well with `holy-lambda`! :)

  5. Empty `.aws` directory created.
  
     **Solution**:
     
     If you see anywhere `.aws` empty directory then remove it and update HL stack.
     
  6. Long running HL `bb commands` on M1 based MacOS.
  
     **Solution**: 
     
     None!
     
     We need to wait for official GraalVM CE Images unfortunetely: 
     https://github.com/oracle/graal/issues/2666
     
  7. bb stack:sync or any other commands hang:
  
     **Possible causes**:
     1. You have M1 based MacOS. See 6)
     2. Docker volume broken state, caused by interrupted copy from remote to host. See solution!
     3. Poor internet connection, which may cause docker volume broken state.
        See solution!
        
     **Solution**
     ```
     bb stack:purge
     ```

  8. I'm using AWS vault and I don't have `~/.aws` folder.
  
     Upon `bb stack:sync` or any other command I see:
     ```
      ❯ bb stack:sync
      [holy-lambda] AWS configuration check failed. Unable to get value from the profile: default

      The config profile (default) could not be found

      Did you run command: aws configure?
     ```
    
     **Solution**
     
     Add `HL_NO_PROFILE=1` environment variable like: `HL_NO_PROFILE=1 bb stack:sync`
  
     
     
     
  
     
