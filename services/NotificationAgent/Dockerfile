FROM discoenv/javabase

ADD target/notificationagent-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "notificationagent-standalone.jar"]
CMD ["--help"]
