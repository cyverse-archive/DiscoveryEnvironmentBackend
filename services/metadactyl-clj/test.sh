#!/bin/sh

set -e
set -x

docker run --name de-db -e POSTGRES_PASSWORD=notprod -d discoenv/de-db
sleep 2
docker run --rm --link de-db:postgres discoenv/de-db-loader
docker run -a stdout -a stderr -i -t --rm -v $(pwd):/build -v ~/.m2:/root/.m2 -w /build --link de-db:postgres clojure lein test
