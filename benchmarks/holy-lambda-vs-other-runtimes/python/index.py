def handler(event, context):
  return {
    "statusCode": 200,
    "body": "Hello world!",
    "headers": {
      "Content-Type": "text/plain; charset=utf-8"
    }
  }
