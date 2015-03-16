FROM discoenv/javabase

ADD target/iplant-email-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "iplant-email-standalone.jar"]
CMD ["--help"]
