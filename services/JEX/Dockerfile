FROM ubuntu:14.04

RUN bash -c "echo 'deb http://research.cs.wisc.edu/htcondor/debian/stable/ wheezy contrib' >> /etc/apt/sources.list.d/condor.list"
RUN apt-get --allow-unauthenticated update && apt-get --allow-unauthenticated install -y condor openjdk-7-jre-headless
RUN usermod -s /bin/bash -d /home/condor/ condor
ADD startup.sh /usr/bin/
USER condor
WORKDIR /home/condor
ADD target/jex-standalone.jar /home/condor/
EXPOSE 60000
CMD ["startup.sh"]

