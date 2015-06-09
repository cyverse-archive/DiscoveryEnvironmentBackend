FROM discoenv/javabase

ADD build/* /home/iplant/resources/
ADD target/kifshare-standalone.jar /home/iplant/
ADD conf/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
USER iplant
ENTRYPOINT ["java", "-cp", ".:kifshare-standalone.jar", "kifshare.core"]
CMD ["--help"]
