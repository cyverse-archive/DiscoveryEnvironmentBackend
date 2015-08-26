#!/bin/sh

set -x
set -e

PGHOST="$POSTGRES_PORT_5432_TCP_ADDR"
PGPORT="$POSTGRES_PORT_5432_TCP_PORT"
PGADMIN="postgres"
PGADMINPASS="$POSTGRES_ENV_POSTGRES_PASSWORD"

cat << EOF >> /root/.pgpass
$PGHOST:$PGPORT:*:postgres:$PGADMINPASS
$PGHOST:$PGPORT:*:de:notprod
EOF

chmod 0600 /root/.pgpass

cp /database.tar.gz /de-db.tar.gz
java -jar /facepalm-standalone.jar -m update -A $PGADMIN -U de -d de -h $PGHOST -p $PGPORT -f /de-db.tar.gz
