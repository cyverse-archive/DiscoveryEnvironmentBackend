FROM discoenv/javabase

COPY target/saved-searches-standalone.jar /home/iplant/
COPY conf/main/log4j.properties /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:saved-searches-standalone.jar", "saved_searches.core"]
CMD ["--help"]
