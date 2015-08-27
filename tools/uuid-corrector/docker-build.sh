#!/bin/sh
set -x
set -e

docker pull discoenv/buildenv:latest
docker run --rm -t -a stdout -a stderr -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/build -w /build discoenv/buildenv lein uberjar
docker build --rm -t $DOCKER_USER/uuid-corrector:dev .
docker push $DOCKER_USER/uuid-corrector:dev
