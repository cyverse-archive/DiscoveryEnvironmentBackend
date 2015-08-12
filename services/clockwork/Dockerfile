FROM discoenv/javabase

COPY target/clockwork-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/clockwork-logging.xml", "-cp", ".:clockwork-standalone.jar", "clockwork.core"]
