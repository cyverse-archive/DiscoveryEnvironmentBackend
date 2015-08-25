FROM ubuntu:14.04

ADD https://everdene.iplantcollaborative.org/jenkins/job/databases-dev/lastSuccessfulBuild/artifact/databases/de-database-schema/database.tar.gz /
ADD https://everdene.iplantcollaborative.org/jenkins/job/databases-dev/lastSuccessfulBuild/artifact/databases/jex-db/jex-db.tar.gz /
ADD https://everdene.iplantcollaborative.org/jenkins/job/databases-dev/lastSuccessfulBuild/artifact/databases/metadata/metadata-db.tar.gz /
ADD https://everdene.iplantcollaborative.org/jenkins/job/databases-dev/lastSuccessfulBuild/artifact/databases/notification-db/notification-db.tar.gz /

RUN apt-get update && apt-get install -y openjdk-7-jre-headless postgresql-client-9.3

COPY target/facepalm-standalone.jar /

ENTRYPOINT ["java", "-jar", "facepalm-standalone.jar"]
CMD [ "--help" ]
