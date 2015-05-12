FROM discoenv/javabase

ADD target/infosquito-standalone.jar /home/iplant/
ADD conf/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:infosquito-standalone.jar", "infosquito.core"]
CMD ["--help"]
