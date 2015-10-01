#!/bin/sh

set -e
set -x

if [ $(docker ps | grep jexdb | wc -l) -gt 0 ]; then
    docker kill jexdb
fi

if [ $(docker ps -a | grep jexdb | wc -l) -gt 0 ]; then
    docker rm jexdb
fi

docker build --rm -t discoenv/jex-db-loader:dev .
docker run -d --name jexdb discoenv/jex-db-loader:dev
sleep 5
docker exec jexdb setup-dev-database.sh
docker commit jexdb discoenv/unittest-jexdb:dev
