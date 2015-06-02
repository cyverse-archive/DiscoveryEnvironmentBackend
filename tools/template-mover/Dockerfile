FROM ubuntu:14.04

RUN apt-get update && apt-get install -y openjdk-7-jre

ADD target/template-mover-standalone.jar /

ENTRYPOINT ["java", "-jar", "template-mover-standalone.jar"]
CMD [ "--help" ]
