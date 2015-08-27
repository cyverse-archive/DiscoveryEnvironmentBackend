#!/bin/sh
set -x
set -e

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')

docker pull $DOCKER_USER/clm-builder:latest
docker run --rm -e "GIT_COMMIT=$(git rev-parse HEAD)" -e "BUILD_USER=$(whoami)" -v $(pwd):/jex-events -v $(pwd)/intra-container-build.sh:/bin/intra-container-build.sh -w /jex-events $DOCKER_USER/clm-builder
docker build --rm -t "$DOCKER_USER/$DOCKER_REPO:dev" .
docker push $DOCKER_USER/$DOCKER_REPO:dev
docker rmi $DOCKER_USER/$DOCKER_REPO:dev
