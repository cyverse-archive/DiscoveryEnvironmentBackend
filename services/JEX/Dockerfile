FROM java:8

ADD target/jex-standalone.jar /jex-standalone.jar
ENTRYPOINT ["java", "-jar", "/jex-standalone.jar"]
CMD ["--help"]
