FROM discoenv/javabase

ADD target/data-info-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
ENTRYPOINT ["java", "-cp", ".:data-info-standalone.jar", "data_info.core"]
CMD ["--help"]
