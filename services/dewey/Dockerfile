FROM discoenv/javabase

COPY target/dewey-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/dewey-logging.xml", "-cp", ".:dewey-standalone.jar", "dewey.core"]
CMD ["--help"]
