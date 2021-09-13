# Deep dive into the Backends
  The API of the HL Custom Runtime allows to target rich amount of Clojure backends via conditional readers. Backend is a platform/library, that provides execution environment, and invokes user code in `AWS Lambda` context. In the documentation we use word backend/runtime interchangeably.

  **Therefore**:
  - `Babashka backend` - means the collection of `bb` command + `bootstrap` script that executes user code and bundled `AWS Lambda` event loop.
  - `Clojure backend` - means a `Docker Image` of OpenJDK (or any other JDK variant) + user code and bundled `AWS Lambda` event loop.
  - `Native backend` - means [GraalVM Substrate VM](https://github.com/oracle/graal/blob/master/docs/reference-manual/native-image/README.md) + `bootstrap` script + user code and bundled `AWS Lambda` event loop.

### Table of contents 
  > :information_source: Some of the backends are complex and require understanding some sophisticated concepts. Backends that should be explained in detail will be linked here.

  - [Native Backend](/native-backend)
