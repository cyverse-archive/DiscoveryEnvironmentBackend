FROM discoenv/javabase

ADD target/saved-searches-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "saved-searches-standalone.jar"]
CMD ["--help"]
