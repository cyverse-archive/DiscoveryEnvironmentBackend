FROM discoenv/javabase

ADD target/saved-searches-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "saved-searches-standalone.jar"]
CMD ["--help"]
