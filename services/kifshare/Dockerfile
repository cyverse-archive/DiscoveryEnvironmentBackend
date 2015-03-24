FROM discoenv/javabase

ADD build/* /iplant/home/resources/
ADD target/kifshare-standalone.jar /home/iplant/
ADD conf/log4j.properties /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:kifshare-standalone.jar", "kifshare.core"]
CMD ["--help"]
