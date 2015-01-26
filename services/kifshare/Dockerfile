FROM java:8

ADD build/* /resources/

ADD target/kifshare-standalone.jar /

CMD ["java", "-jar", "kifshare-standalone.jar", "--help"]
