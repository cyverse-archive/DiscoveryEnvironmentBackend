FROM ubuntu:14.04

RUN useradd -m -U -s /bin/bash iplant
ADD condor-log-monitor /bin/
USER iplant
WORKDIR /home/iplant
EXPOSE 60000
ENTRYPOINT ["condor-log-monitor"]
CMD ["--help"]
