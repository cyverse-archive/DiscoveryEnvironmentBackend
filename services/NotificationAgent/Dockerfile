FROM discoenv/javabase

USER root
VOLUME ["/etc/iplant/de"]

COPY conf/main/logback.xml /home/iplant/
COPY target/notificationagent-standalone.jar /home/iplant/
RUN chown -R iplant:iplant /home/iplant/

USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/notificationagent-logging.xml", "-cp", ".:notificationagent-standalone.jar", "notification_agent.core"]
CMD ["--help"]
