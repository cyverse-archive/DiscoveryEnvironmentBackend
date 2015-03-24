FROM discoenv/javabase

ADD target/clockwork-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "clockwork-standalone.jar"]
CMD ["--help"]
