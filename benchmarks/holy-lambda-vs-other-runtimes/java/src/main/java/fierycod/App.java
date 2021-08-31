package fierycod;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class App implements RequestStreamHandler {

  @Override
  public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
    String response = "{\"body\":\"Hello world!\",\"statusCode\":\"200\",\"isBase64Encoded\":\"false\"}";

    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    outputStream.close();
  }
}
