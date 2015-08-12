FROM discoenv/javabase

COPY target/tree-urls-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/tree-urls-logging.xml", "-cp", ".:tree-urls-standalone.jar", "tree_urls.core"]
CMD ["--help"]
