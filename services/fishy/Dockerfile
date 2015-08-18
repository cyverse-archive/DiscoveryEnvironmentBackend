FROM discoenv/javabase

ADD target/fishy-standalone.jar /home/iplant/
ADD conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
VOLUME ["/etc/iplant/de"]
EXPOSE 60000
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/fishy-logging.xml", "-cp", ".:fishy-standalone.jar:/home/iplant/", "fishy.core"]
CMD ["--help"]
