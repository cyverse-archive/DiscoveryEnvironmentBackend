FROM discoenv/javabase

ADD target/metadactyl-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
USER root
RUN mkdir -p /var/log/metadactyl/ && chmod -R a+rw /var/log/metadactyl/
RUN chown iplant:iplant /home/iplant/log4j.properties && chown iplant:iplant /home/iplant/metadactyl-standalone.jar
USER iplant
ENTRYPOINT ["java", "-cp", "/home/iplant/metadactyl-standalone.jar:/home/iplant/", "metadactyl.core"]
CMD ["--help"]