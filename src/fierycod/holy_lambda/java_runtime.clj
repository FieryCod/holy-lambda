(ns ^:no-doc ^:private fierycod.holy-lambda.java-runtime
  (:require
   [fierycod.holy-lambda.util :as u])
  (:import
   [com.amazonaws.services.lambda.runtime Context CognitoIdentity ClientContext Client]))

(defn java-ctx-object->ctx-edn
  [^Context context envs]
  (u/ctx envs
         (fn [] (.getRemainingTimeInMillis context))
         (.getFunctionName context)
         (.getFunctionVersion context)
         (.getInvokedFunctionArn context)
         (.getMemoryLimitInMB context)
         (.getAwsRequestId context)
         (.getLogGroupName context)
         (.getLogStreamName context)
         (when-let [^CognitoIdentity _identity (.getIdentity context)]
           {:identityId (.getIdentityId _identity)
            :identityPoolId (.getIdentityPoolId _identity)})
         (when-let [^ClientContext client-context (.getClientContext context)]
           {:client (when-let [^Client client (.getClient client-context)]
                      {:installationId (.getInstallationId client)
                       :appTitle (.getAppTitle client)
                       :appVersionName (.getAppVersionName client)
                       :appVersionCode (.getAppVersionCode client)
                       :appPackageName (.getAppPackageName client)})
            :custom (into {} (.getCustom client-context))
            :environment (into {} (.getEnvironment client-context))})))

(defn gen-lambda-class-with-prefix
  [prefix gfullname]
  `(gen-class
    :name ~gfullname
    :prefix ~prefix
    :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler]))
