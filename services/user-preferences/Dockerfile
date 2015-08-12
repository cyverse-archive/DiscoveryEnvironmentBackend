FROM discoenv/javabase

COPY target/user-preferences-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/user-preferences-logging.xml", "-cp", ".:user-preferences-standalone.jar", "user_preferences.core"]
CMD ["--help"]
