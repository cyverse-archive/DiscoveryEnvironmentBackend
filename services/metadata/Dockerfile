FROM discoenv/javabase

ADD target/metadata-standalone.jar /home/iplant/
ADD conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
VOLUME ["/etc/iplant/de"]
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/metadata-logging.xml", "-cp", ".:metadata-standalone.jar:/home/iplant/", "metadata.core"]
CMD ["--help"]
