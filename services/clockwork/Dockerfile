FROM discoenv/javabase

ADD target/clockwork-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "clockwork-standalone.jar"]
CMD ["--help"]