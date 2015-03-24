FROM discoenv/javabase

ADD target/donkey-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:donkey-standalone.jar", "donkey.core"]
CMD ["--help"]
