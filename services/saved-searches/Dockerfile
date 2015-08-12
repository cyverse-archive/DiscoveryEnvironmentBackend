FROM discoenv/javabase

COPY target/saved-searches-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/saved-searches-logging.xml", "-cp", ".:saved-searches-standalone.jar", "saved_searches.core"]
CMD ["--help"]
