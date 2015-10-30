#!/bin/sh

set -x
set -e

java -jar /facepalm-standalone.jar -m update -A $POSTGRES_USER -U $POSTGRES_USER -d de -h 127.0.0.1 -p 5432 -f /database.tar.gz
