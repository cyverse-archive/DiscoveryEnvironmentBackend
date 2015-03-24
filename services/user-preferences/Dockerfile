FROM discoenv/javabase

ADD target/user-preferences-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "user-preferences-standalone.jar"]
CMD ["--help"]
