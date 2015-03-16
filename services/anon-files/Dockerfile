FROM discoenv/javabase

ADD target/anon-files-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "anon-files-standalone.jar"]
CMD ["--help"]
