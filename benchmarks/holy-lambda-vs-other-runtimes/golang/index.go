package main

import "github.com/aws/aws-lambda-go/lambda"

type Response struct {
	StatusCode      int    `json:"statusCode"`
	Body            string `json:"body"`
	IsBase64Encoded bool   `json:"isBase64Encoded"`
}

func hello() (Response, error) {
	return Response{
		StatusCode:      200,
		Body:            "Hello world!",
		IsBase64Encoded: false,
	}, nil
}

func main() {
	lambda.Start(hello)
}
