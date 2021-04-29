package com.amazonaws.services.lambda.runtime;

import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.ClientContext;

public class Context {
  public int getRemainingTimeInMillis() { return 0; }
  public String getFunctionName() { return ""; }
  public String getFunctionVersion() { return ""; }
  public String getInvokedFunctionArn() { return ""; }
  public String getMemoryLimitInMB() { return ""; }
  public String getAwsRequestId() { return ""; }
  public String getLogGroupName() { return ""; }
  public String getLogStreamName() { return ""; }
  public ClientContext getClientContext() { return new ClientContext(); }
  public CognitoIdentity getIdentity() { return new CognitoIdentity(); }
}
