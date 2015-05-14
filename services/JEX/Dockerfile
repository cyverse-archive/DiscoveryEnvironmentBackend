FROM java:8

COPY target/jex-standalone.jar /jex-standalone.jar
COPY conf/log4j2.xml /log4j2.xml
ENTRYPOINT ["java", "-jar", "/jex-standalone.jar"]
CMD ["--help"]
