#!/bin/sh

set -e
set -x

if [ $(docker ps | grep dedb | wc -l) -gt 0 ]; then
    docker kill dedb
fi

if [ $(docker ps -a | grep dedb | wc -l) -gt 0 ]; then
    docker rm dedb
fi

docker build --rm -t discoenv/de-db-loader:dev .
docker run -d --name dedb discoenv/de-db-loader:dev
sleep 5
docker exec dedb setup-dev-database.sh
docker commit dedb discoenv/unittest-dedb:dev
