FROM discoenv/javabase

ADD target/metadata-standalone.jar /home/iplant/
ADD conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
VOLUME ["/etc/iplant/de"]
USER iplant
ENTRYPOINT ["java", "-cp", ".:metadata-standalone.jar:/home/iplant/", "metadata.core"]
CMD ["--help"]
