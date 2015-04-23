#!/bin/sh
set -x
set -e

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')

docker run --rm -t -a stdout -a stderr -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/build -v ~/.m2:/root/.m2 -w /build discoenv/buildenv ./intra-container-build.sh
docker build --rm -t "$DOCKER_USER/$DOCKER_REPO:dev" .
docker push $DOCKER_USER/$DOCKER_REPO:dev
