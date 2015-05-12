FROM discoenv/javabase

ADD target/data-info-standalone.jar /home/iplant/
ADD conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:data-info-standalone.jar", "data_info.core"]
CMD ["--help"]
