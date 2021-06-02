exports.handler = async function (_event, _context) {
  return {
    statusCode: 200,
    body: "Hello world!",
    headers: {
      "Content-Type":"text/plain; charset=utf-8"
    }
  }
};
