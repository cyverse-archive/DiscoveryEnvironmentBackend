#!/bin/sh
set -x
set -e

docker run --rm -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/build -w /build $DOCKER_USER/buildenv lein uberjar
docker build --rm -t $DOCKER_USER/porklock:dev .
docker push $DOCKER_USER/porklock:dev
