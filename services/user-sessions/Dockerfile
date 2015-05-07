FROM discoenv/javabase

COPY target/user-sessions-standalone.jar /home/iplant/
COPY conf/main/log4j.properties /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:user-sessions-standalone.jar", "user_sessions.core"]
CMD ["--help"]
