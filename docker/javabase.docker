FROM jeanblanchard/java:8
MAINTAINER Ian McEwen <mian@iplantcollaborative.org>

RUN apk add --update curl \
    && mkdir -p /tmp/jce \
    && curl -jksSLH "Cookie: oraclelicense=accept-securebackup-cookie" -o /tmp/jce/jce.zip http://download.oracle.com/otn-pub/java/jce/8/jce_policy-8.zip \
    && unzip -d /tmp/jce -o /tmp/jce/jce.zip \
    && cp /tmp/jce/UnlimitedJCEPolicyJDK8/*.jar /opt/jdk/jre/lib/security/ \
    && rm -rf /tmp/jce \
    && apk del curl \
    && rm -rf /var/cache/apk/*

RUN mkdir -p /home/iplant
RUN busybox adduser -s /bin/sh -u 1337 -D -h /home/iplant iplant

USER iplant
RUN mkdir -p /home/iplant/logs/
WORKDIR /home/iplant
EXPOSE 60000
