FROM discoenv/javabase

ADD target/donkey-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "donkey-standalone.jar"]
CMD ["--help"]
