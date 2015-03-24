FROM discoenv/javabase

ADD target/dewey-standalone.jar /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-jar", "dewey-standalone.jar"]
CMD ["--help"]
