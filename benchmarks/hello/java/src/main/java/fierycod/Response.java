package fierycod;

import java.util.HashMap;
import java.util.Map;

class Response<T> {
  public Map<String, String> headers;
  public T body;
  public int statusCode = 200;

  Response(T newBody) {
    this.headers = new HashMap<>();
    this.setBody(newBody);
  }

  public Response<T> setStatusCode(int newStatusCode) {
    this.statusCode = newStatusCode;
    return this;
  }

  public Response<T> setHeader(String k, String v) {
    this.headers.put(k.toLowerCase(), v);
    return this;
  }

  public void setBody(T newBody) {
    this.body = newBody;
  }

}
