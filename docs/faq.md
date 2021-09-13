# (FAQ) Frequently Asked Questions 

## What is native backend?
   Native backend/runtime is GraalVM native-image compiled version of AWS Custom Runtime. Native runtime is entirely written in Clojure and upon `hl:native:executable` packed with the user code. 
   
   The output artifact of this operation is `zip` which can be deployed with any of the available deployment tools. 
   
## Can I use HL with CDK, Pulumi, Terraform?
   Sure! HL doesn't stop you from using any of above deployment tools. All you need is the knowledge how to build deployable artifacts, that you can reference in your deployment descriptor.
  
   **Deployable artifact coordinate according to backend**
  
   - Clojure - > `.holy-lambda/build/output.jar`
   
     **Build command**
     ```bash
     bb hl:sync && bb hl:compile
     ```

   - Native -> `.holy-lambda/build/latest.zip`
   
     **Build command**
     ``` 
     bb hl:sync && bb hl:compile && bb hl:native:executable
     ```
     
     **Content of latest.zip**
     - `bootstrap` - a script that executes the initializes the runtime and executes the binary
     - `output` - native compiled Clojure user code + Custom runtime
     - `native-deps` - dependencies of the application
     
   - Babashka -> `src` (Application sources provided as is)

     Babashka backend is provided as a serverless layer which you have to deploy manually and reference it's ARN in deployment descriptor.
     1. Deploy the [layer](https://serverlessrepo.aws.amazon.com/applications/eu-central-1/443526418261/holy-lambda-babashka-runtime)
       and get it's ARN.
     2. Reference `src` as the `CodeUri` of your application.
     3. Set runtime to `provided` and reference ARN of the layer in `layers` option.
     4. Additional Clojure deps should be packed to a layer. Clojure deps are stored at: `.holy-lambda/bb-clj-deps`.
     5. Additional babashka pods should be packed to a layer. Babashka pods are stored at `.holy-lambda/pods`.
    
## Is HL runtime asynchronous?
   Nope! Native, Babashka and Java runtimes are synchronous, but handlers can be asynchronous for convenience and return either:
   
     - `Channel<Map|String|ByteArray|nil>`
     - `Future<Map|String|ByteArray|nil>`
     - `Promise<Map|String|ByteArray|nil>`
   
   Still though the runtime will wait for the result of asynchronous handler, before taking a new task to process. Scaling lambdas should be achieved via concurrency option. More info [here](https://docs.aws.amazon.com/lambda/latest/dg/configuration-concurrency.html)
   
   
   
  

