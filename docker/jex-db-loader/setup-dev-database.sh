#!/bin/bash

set -x
set -e

psql -h 127.0.0.1 -U $POSTGRES_USER -d postgres -c "CREATE DATABASE jex WITH OWNER $POSTGRES_USER;"
java -jar /facepalm-standalone.jar -m init -A $POSTGRES_USER -U $POSTGRES_USER -h 127.0.0.1 -d jex -f /jex-db.tar.gz
