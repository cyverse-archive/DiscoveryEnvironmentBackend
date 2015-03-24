FROM discoenv/javabase

ADD target/donkey-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
ENTRYPOINT ["java", "-cp", ".:donkey-standalone.jar", "donkey.core"]
CMD ["--help"]
