FROM discoenv/javabase

ADD build/* /iplant/home/resources/
ADD target/kifshare-standalone.jar /home/iplant/
ENTRYPOINT ["java", "-jar", "kifshare-standalone.jar"]
CMD ["--help"]
