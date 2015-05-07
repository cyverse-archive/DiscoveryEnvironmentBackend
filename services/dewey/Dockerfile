FROM discoenv/javabase

COPY target/dewey-standalone.jar /home/iplant/
COPY resources/log4j.properties /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:dewey-standalone.jar", "dewey.core"]
CMD ["--help"]
