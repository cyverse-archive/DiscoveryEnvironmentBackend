FROM discoenv/javabase

COPY target/tree-urls-standalone.jar /home/iplant/
COPY conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:tree-urls-standalone.jar", "tree_urls.core"]
CMD ["--help"]
