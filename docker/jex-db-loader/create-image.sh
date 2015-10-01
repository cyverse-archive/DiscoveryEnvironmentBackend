#!/bin/sh

set -e
set -x

if [ $(docker ps | grep jex-db | wc -l) -gt 0 ]; then
    docker kill jex-db
fi

if [ $(docker ps -a | grep jex-db | wc -l) -gt 0 ]; then
    docker rm jex-db
fi

docker build --rm -t discoenv/jex-db-loader:dev .
docker run --name jex-db -e POSTGRES_PASSWORD=notprod -d -p 5432:5432 discoenv/de-db
sleep 5
docker run --rm --link jex-db:postgres discoenv/jex-db-loader:dev
docker kill jex-db
docker commit jex-db discoenv/unittest-jexdb:dev
