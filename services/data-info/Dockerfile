FROM discoenv/javabase

ADD target/data-info-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "data-info-standalone.jar"]
CMD ["--help"]
