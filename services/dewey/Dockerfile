FROM discoenv/javabase

ADD target/dewey-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "dewey-standalone.jar"]
CMD ["--help"]