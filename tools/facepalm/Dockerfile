FROM java:8

ADD target/facepalm-standalone.jar /

RUN apt-get update && apt-get install -y postgresql-client

CMD ["java", "-jar", "facepalm-standalone.jar"]