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
   
      **Solution**:
      Increase the RAM limit in Docker UI [preferences](https://docs.docker.com/docker-for-mac/#resources).
      
  4. Command `bb stack:invoke` fails with the following message
      ```
      Mounting /path-to-source/holy-lambda-example/src as /var/task:ro,delegated inside runtime container
      START RequestId: 4dc6fcf5-7db2-4854-bba7-abae8038ef8f Version: $LATEST
      time="2021-05-17T16:52:48.278" level=error msg="Init failed" InvokeID= error="Couldn't find valid bootstrap(s): [/var/task/bootstrap /opt/bootstrap /var/runtime/bootstrap]"
      time="2021-05-17T16:52:48.279" level=error msg="INIT DONE failed: Runtime.InvalidEntrypoint"
      ```
  
      The layers of your template have not been configured correctly. Ensure that stack:sync reports an ARN and it has been added to the `template.yml`
      
  5. Could not find `/project/.holy-lambda/clojure-tools-1.10.3.849.jar`
     Ignore this. Nothing bad happens!
