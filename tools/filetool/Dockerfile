FROM alpine:3.2

RUN apk --update add openjdk7-jre

ADD target/porklock-standalone.jar /porklock-standalone.jar

ENTRYPOINT ["java", "-jar", "/porklock-standalone.jar"]

CMD ["--help"]
