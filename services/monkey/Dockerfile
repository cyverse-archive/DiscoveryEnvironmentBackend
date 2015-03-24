FROM discoenv/javabase

ADD target/monkey-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
ENTRYPOINT ["java", "-cp", ".:monkey-standalone.jar", "monkey.core"]
CMD ["--help"]
