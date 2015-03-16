FROM discoenv/javabase

ADD target/user-preferences-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "user-preferences-standalone.jar"]
CMD ["--help"]
