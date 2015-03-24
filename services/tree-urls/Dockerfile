FROM discoenv/javabase

ADD target/tree-urls-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "tree-urls-standalone.jar"]
CMD ["--help"]
