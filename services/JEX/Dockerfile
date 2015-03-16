FROM discoenv/javabase

ADD target/jex-standalone.jar /home/iplant/jex-standalone.jar
ENTRYPOINT ["java", "-jar", "jex-standalone.jar"]
CMD ["--help"]
