FROM ubuntu
MAINTAINER John Wregglesworth <wregglej@gmail.com>

RUN apt-get update
RUN apt-get install -y openjdk-7-jre-headless

ADD target/anon-files-3.1.0-standalone.jar anon-files-standalone.jar

EXPOSE 31300

CMD java -jar anon-files-standalone.jar
