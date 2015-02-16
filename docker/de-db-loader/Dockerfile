FROM discoenv/facepalm

COPY setup-dev-database.sh /bin/setup-dev-database.sh
COPY update-dev-database.sh /bin/update-dev-database.sh
COPY database.tar.gz /database.tar.gz

CMD ["setup-dev-database.sh"]