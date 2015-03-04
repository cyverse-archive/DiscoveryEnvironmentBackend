FROM java:8

RUN sh -c "echo deb http://research.cs.wisc.edu/htcondor/debian/stable/ wheezy contrib > /etc/apt/sources.list.d/condor.list"
RUN apt-get update && apt-get --allow-unauthenticated install -y condor
ADD target/jex-standalone.jar /home/condor/
RUN chown -R condor:condor /home/condor/

USER condor
ENV HOME /home/condor
WORKDIR /home/condor

CMD ["java", "-jar", "jex-standalone.jar", "--help"]