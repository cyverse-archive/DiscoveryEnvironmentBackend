FROM discoenv/javabase

ADD target/user-sessions-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "user-sessions-standalone.jar"]
CMD ["--help"]
