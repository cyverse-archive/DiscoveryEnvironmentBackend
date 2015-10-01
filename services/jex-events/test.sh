#!/bin/sh

set -e
set -x

if [ $(docker ps | grep jexdb | wc -l) -eq 0]; then
    echo "Can't find a container named 'jexdb'. Going to try and create it..."

    if [ $(docker images | grep 'unittest-jexdb:dev' | wc -l) -eq 0 ]; then
        echo "Please run create-image.sh in docker/jex-db-loader/"
        exit 1
    fi

    docker run --name jexdb -d discoenv/unittest-jexdb:dev
    sleep 5
fi

JEXDB="postgres://de:notprod@$(docker inspect -f '{{.NetworkSettings.IPAddress}}' jexdb):5432/jex?sslmode=disable" gb test -test.v
