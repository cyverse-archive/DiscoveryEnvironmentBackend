FROM discoenv/javabase

ADD target/monkey-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "monkey-standalone.jar"]
CMD ["--help"]
