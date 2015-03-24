FROM discoenv/javabase

ADD target/notificationagent-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:notificationagent-standalone.jar", "notification_agent.core"]
CMD ["--help"]
