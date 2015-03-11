FROM discoenv/facepalm

COPY setup-dev-database.sh /bin/setup-dev-database.sh
COPY update-dev-database.sh /bin/update-dev-database.sh
COPY database.tar.gz /database.tar.gz
COPY facepalm-standalone.jar /facepalm-standalone.jar

CMD ["setup-dev-database.sh"]