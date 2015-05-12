FROM discoenv/javabase

COPY target/anon-files-standalone.jar /home/iplant/
COPY resources/main/log4j2.xml /home/iplant/log4j2.xml
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:anon-files-standalone.jar", "anon_files.core"]
CMD ["--help"]
