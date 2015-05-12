FROM discoenv/javabase

ADD target/info-typer-standalone.jar /home/iplant/
ADD conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:info-typer-standalone.jar", "info_typer.core"]
CMD ["--help"]
