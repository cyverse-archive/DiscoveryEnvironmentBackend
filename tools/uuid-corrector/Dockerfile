FROM ubuntu:14.04

RUN apt-get update && apt-get install -y openjdk-7-jre

ADD target/uuid-corrector-standalone.jar /

ENTRYPOINT ["java", "-jar", "uuid-corrector-standalone.jar"]
CMD [ "--help" ]
