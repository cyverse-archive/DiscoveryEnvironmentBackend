FROM discoenv/javabase

COPY target/user-sessions-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/user-sessions-logging.xml", "-cp", ".:user-sessions-standalone.jar", "user_sessions.core"]
CMD ["--help"]
