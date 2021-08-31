FROM amazonlinux

MAINTAINER Karol Wójcik <karol.wojcik@tuta.io>
RUN yum install golang -y

COPY go.sum .
COPY go.mod .

RUN go get "github.com/aws/aws-lambda-go/lambda"

COPY index.go .

RUN go build
