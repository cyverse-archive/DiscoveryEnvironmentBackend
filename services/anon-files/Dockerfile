FROM discoenv/javabase

COPY target/anon-files-standalone.jar /home/iplant/
COPY resources/main/log4j.properties /home/iplant/log4j.properties
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:anon-files-standalone.jar", "anon_files.core"]
CMD ["--help"]
