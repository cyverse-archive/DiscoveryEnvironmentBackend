FROM discoenv/javabase

ADD target/notificationagent-standalone.jar /home/iplant/
ADD conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/notificationagent-logging.xml", "-cp", ".:notificationagent-standalone.jar", "notification_agent.core"]
CMD ["--help"]
