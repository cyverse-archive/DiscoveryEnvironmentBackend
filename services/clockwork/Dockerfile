FROM discoenv/javabase

COPY target/clockwork-standalone.jar /home/iplant/
COPY resources/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:clockwork-standalone.jar", "clockwork.core"]
CMD ["--help"]
