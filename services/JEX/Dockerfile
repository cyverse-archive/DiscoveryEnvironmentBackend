FROM java:8

COPY target/jex-standalone.jar /jex-standalone.jar
COPY conf/main/logback.xml /logback.xml
COPY submitnerator.sh /submitnerator.sh
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/jex-logging.xml", "-cp", ".:jex-standalone.jar", "jex.core"]
CMD ["--help"]
