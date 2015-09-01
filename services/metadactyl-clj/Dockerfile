FROM discoenv/javabase

COPY target/metadactyl-standalone.jar /home/iplant/
COPY conf/main/logback.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/ && \
    mkdir -p /etc/iplant/de/crypto && \
    touch /etc/iplant/de/crypto/pubring.gpg && \
    touch /etc/iplant/de/crypto/random_seed && \
    touch /etc/iplant/de/crypto/secring.gpg && \
    touch /etc/iplant/de/crypto/trustdb.gpg && \
    chown -R iplant:iplant /etc/iplant/de/crypto
VOLUME ["/etc/iplant/de"]
USER iplant
ENTRYPOINT ["java", "-Dlogback.configurationFile=/etc/iplant/de/logging/metadactyl-logging.xml", "-cp", ".:metadactyl-standalone.jar:/home/iplant/", "metadactyl.core"]
CMD ["--help"]
