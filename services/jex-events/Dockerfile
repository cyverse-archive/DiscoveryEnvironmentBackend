FROM ubuntu:14.04

RUN useradd -m -U -s /bin/bash iplant
ADD jex-events /bin/
USER iplant
WORKDIR /home/iplant
EXPOSE 60000
ENTRYPOINT ["jex-events"]
CMD ["--help"]
