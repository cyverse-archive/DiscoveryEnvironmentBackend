FROM discoenv/javabase

ADD target/iplant-groups-standalone.jar /home/iplant/
ADD conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
VOLUME ["/etc/iplant/de"]
EXPOSE 60000
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/iplant-groups-logging.xml", "-cp", ".:iplant-groups-standalone.jar:/home/iplant/", "iplant_groups.core"]
CMD ["--help"]
