package fierycod;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.api.client.AWSLambda;

/**
 * Lambda function entry point. You can change to use other pojo type or implement
 * a different RequestHandler.
 *
 * @see <a href=https://docs.aws.amazon.com/lambda/latest/dg/java-handler.html>Lambda Java Handler</a> for more information
 */
public class App implements RequestHandler<Object, Response<String>> {

  public App() {
  }

  @Override
  public Response<String> handleRequest(final Object input, final Context context) {
    return new Response<String>("Hello world!").setHeader("content-type", "plain/text");
  }
}
