using System;
using Amazon.Lambda.Core;
using Amazon.Lambda.Serialization.Json;
using System.Threading.Tasks;
using System.Collections.Generic;

namespace dotnet21
{
    public class Hello
    {
        [LambdaSerializer(typeof(JsonSerializer))]
        public async Task<Object> Handler(Object request)
        {
        
            return new
            {
                statusCode = 200,
                body = "Hello world!",
                headers = new Dictionary<string, string> {
                   {"Content-Type", "text/plain"}
                }
            };
        }
    }
}
