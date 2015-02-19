#!/bin/sh

echo "This script and docker container are intended for use against the database created by the ./test.sh script."
echo ''
echo "The password for the 'de' user to the 'de' database is 'notprod' (without the quotes)."

docker run -it --link de-db:postgres --rm postgres sh -c 'exec psql -h "$POSTGRES_PORT_5432_TCP_ADDR" -p "$POSTGRES_PORT_5432_TCP_PORT" -U de -d de'
