FROM discoenv/javabase

ADD target/infosquito-standalone.jar /home/iplant/
ADD conf/log4j.properties /home/iplant/
ENTRYPOINT ["java", "-cp", ".:infosquito-standalone.jar", "infosquito.core"]
CMD ["--help"]
