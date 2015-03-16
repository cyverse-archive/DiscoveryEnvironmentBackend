FROM discoenv/javabase

ADD target/infosquito-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "infosquito-standalone.jar"]
CMD ["--help"]
