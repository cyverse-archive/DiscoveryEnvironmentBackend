FROM ubuntu:14.04

RUN apt-get update && apt-get install -y openjdk-7-jre postgresql-client-9.3

ADD target/facepalm-standalone.jar /

ENTRYPOINT ["java", "-jar", "facepalm-standalone.jar"]
CMD [ "--help" ]
