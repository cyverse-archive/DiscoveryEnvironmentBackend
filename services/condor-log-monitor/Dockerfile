FROM ubuntu:14.04

RUN useradd -m -U -s /bin/bash iplant
ADD bin/condor-log-monitor /bin/
USER iplant
RUN mkdir -p /home/iplant/logs/
WORKDIR /home/iplant
EXPOSE 60000
ENTRYPOINT ["condor-log-monitor"]
CMD ["--help"]
