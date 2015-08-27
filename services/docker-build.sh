#!/bin/sh
set -x
set -e

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')

docker pull $DOCKER_USER/buildenv:latest
docker run --rm -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/build -w /build $DOCKER_USER/buildenv lein uberjar
docker build --rm -t "$DOCKER_USER/$DOCKER_REPO:dev" .
docker push $DOCKER_USER/$DOCKER_REPO:dev
docker rmi $DOCKER_USER/$DOCKER_REPO:dev
