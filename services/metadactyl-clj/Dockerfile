FROM discoenv/javabase

ADD target/metadactyl-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
ENTRYPOINT ["java", "-cp", ".:metadactyl-standalone.jar:/home/iplant/", "metadactyl.core"]
CMD ["--help"]
