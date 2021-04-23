package com.amazonaws.services.lambda.runtime;

import com.amazonaws.services.lambda.runtime.Client;

public class ClientContext {
  public Client getClient() { return new Client(); }
  public void getEnvironment() {}
  public void getCustom() {}
}
