FROM discoenv/javabase

ADD target/user-sessions-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "user-sessions-standalone.jar"]
CMD ["--help"]
