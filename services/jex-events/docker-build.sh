#!/bin/sh
set -x
set -e

DOCKER_USER=discoenv
VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')

docker run --rm -t -a stdout -a stderr -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/jex-events -w /jex-events discoenv/clm-builder
docker build --rm -t "$DOCKER_USER/$DOCKER_REPO:dev" .
docker push $DOCKER_USER/$DOCKER_REPO:dev
