FROM discoenv/javabase

ADD target/anon-files-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "anon-files-standalone.jar"]
CMD ["--help"]
