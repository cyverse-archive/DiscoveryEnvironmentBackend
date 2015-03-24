FROM discoenv/javabase

ADD target/info-typer-standalone.jar /home/iplant/
ADD conf/main/log4j.properties /home/iplant/
ENTRYPOINT ["java", "-cp", ".:info-typer-standalone.jar", "info_typer.core"]
CMD ["--help"]
