FROM discoenv/facepalm:dev

ADD setup-pg-hbaconf.sh /docker-entrypoint-initdb.d/
COPY setup-dev-database.sh /bin/setup-dev-database.sh
COPY update-dev-database.sh /bin/update-dev-database.sh
COPY pgpass /root/.pgpass
RUN chmod 0600 /root/.pgpass
RUN mkdir /data
ENV POSTGRES_PASSWORD=notprod POSTGRES_USER=de PGDATA=/data
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["postgres"]
