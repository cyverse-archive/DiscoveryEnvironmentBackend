FROM discoenv/javabase

ADD target/tree-urls-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "tree-urls-standalone.jar"]
CMD ["--help"]
