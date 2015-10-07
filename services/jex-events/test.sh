#!/bin/sh

set -e
set -x

docker-compose -f test.yml up --force-recreate
