#!/bin/bash

set -x
set -e


env

PGHOST="$POSTGRES_PORT_5432_TCP_ADDR"
PGPORT="$POSTGRES_PORT_5432_TCP_PORT"
PGADMIN="postgres"
PGADMINPASS="$POSTGRES_ENV_POSTGRES_PASSWORD"

cat << EOF >> ~/.pgpass
$PGHOST:$PGPORT:*:postgres:$PGADMINPASS
$PGHOST:$PGPORT:*:de:notprod
EOF

chmod 0600 ~/.pgpass

psql -h $PGHOST -p $PGPORT -U $PGADMIN -c "CREATE ROLE de WITH PASSWORD 'notprod' LOGIN;"
psql -h $PGHOST -p $PGPORT -U $PGADMIN -c "CREATE DATABASE de WITH OWNER de;"

java -jar /facepalm-standalone.jar -m init -A $PGADMIN -U de -d de -h $PGHOST -p $PGPORT -f /database.tar.gz
