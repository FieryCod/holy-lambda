FROM openjdk:latest
MAINTAINER Karol Wójcik <karol.wojcik@tuta.io>

ADD .holy-lambda/build/output.jar output.jar

CMD java -jar output.jar "com.company.example-lambda.core.ExampleLambda"
