FROM discoenv/javabase

COPY target/user-preferences-standalone.jar /home/iplant/
COPY conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:user-preferences-standalone.jar", "user_preferences.core"]
CMD ["--help"]
