package fierycod;

import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;

public class Entrypoint {
  public static void main(String[] args) {
    AWSLambda.main(new String[]{"fierycod.App::handleRequest"});
  }
}
