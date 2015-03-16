FROM discoenv/javabase

ADD target/metadactyl-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "metadactyl-standalone.jar"]
CMD ["--help"]