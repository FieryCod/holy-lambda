# Data Model

**HL Request Handler**
```clojure
Handler<HLEvent, HLContext> => HLResponse 
```

**Basic handler**
```clojure
(defn BasicHandler
  [{:keys [event ctx]}]
  {:statusCode 200
   :headers {"content-type" "application/json"}
   :body {:hello "world"})
```

**[Runtime/Backend initialization](/api)**

```clojure
(h/entrypoint [HandlerVar] opts)
```

## HLContext
  **Map of**
  
  | Keyword                 | Value Type                   | Description                                                                      |
  |-------------------------|------------------------------|----------------------------------------------------------------------------------|
  | `:getRemainingTimeInMs` | `(fn [] remaining-time)`     | Gets remaining time of Lambda execution                                          |
  | `:fnName`               | `string`                     | Name of the function                                                             |
  | `:fnVersion`            | `string`                     | Function version                                                                 |
  | `:fnInvokedArn`         | `string`                     | ARN of the function                                                              |
  | `:memoryLimitInMb`      | `string`                     | Limit of the function memory                                                     |
  | `:awsRequestId`         | `string`                     | Id of the request (useful as a correlation id                                    |
  | `:logGroupName`         | `string`                     | Log group name                                                                   |
  | `:logStreamName`        | `string`                     | Log stream name                                                                  |
  | `:identity`             | `Option<CognitoIdentity>`    | Information about the Amazon Cognito identity that authorized the request        |
  | `:clientContext`        | `ClientContext`              | (mobile apps) Client context that's provided to Lambda by the client application |
  | `:envs`                 | `ClojureMap<string, string>` | All environment variables of the application                                     |

#### CognitoIdentity
  **Map of**
  
  | Keyword                  | Value Type | Description                                                     |
  |--------------------------|------------|-----------------------------------------------------------------|
  | `:cognitoIdentityId`     | `string`   | The authenticated Amazon Cognito identity                       |
  | `:cognitoIdentityPoolId` | `string`   | The Amazon Cognito identity pool that authorized the invocation |

#### ClientContext
  **Map of**
  
  | Keyword                 | Value Type | Description                                          |
  |-------------------------|------------|------------------------------------------------------|
  | `:installation_id`      | `string`   | Application installation ID                          |
  | `:app_title`            | `string`   | App title                                            |
  | `:app_version_name`     | `string`   | App version name                                     |
  | `:app_version_code`     | `string`   | App version code                                     |
  | `:app_package_name`     | `string`   | App package name                                     |
  | `:env.platform`         | `string`   | Platfom                                              |
  | `:env.platform_version` | `string`   | Platform version                                     |
  | `:env.make`             | `string`   | ?                                                    |
  | `:env.model`            | `string`   | ?                                                    |
  | `:env.locale`           | `string`   | ?                                                    |
  | `:Custom`               | `string`   | Custom properties that can be set by the application |

## HLEvent
  Depends on the event provider. Every service integrated with AWS Lambda may produce different shape of the event.

## HLResponse
 1. `nil` (indicates the ACK in SQS) OR
 2. `Map` OR
 
     **Map of**
     
     | Keyword              | Value Type                                     |
     |----------------------|------------------------------------------------|
     | `:statusCode`        | `int`                                          |
     | `:headers`           | `ClojureMap<string,string>`                    |
     | `:multiValueHeaders` | `ClojureMap<string,vector<string>`             |
     | `:body`              | `string\|nil\|boolean\|vector<any>\|list<any>` |
     | `isBase64Encoded`    | `boolean`                                      |
 
 3. `ByteArray` OR
     
     Stringified `Map` (2) converted to `ByteArray`.
     
 4. `Future<Map|String|ByteArray|nil>` OR
 
     Composite type of `Future` + primitive.
 5. `ClojurePromise<Map|String|ByteArray|nil>` OR
 
     Composite type of `ClojurePromise` + primitive.
 6. `Channel<Map|String|ByteArray|nil` (only with [async-retriever](/stable-releases?id=stable-releases))
     
     Composite type of `Channel` + primitive.

 
  
