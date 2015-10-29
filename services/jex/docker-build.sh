#!/bin/sh
set -x
set -e

if [ -z "$DOCKER_USER" ]; then
	DOCKER_USER=discoenv
fi

if [ -z "$DOCKER_REPO" ]; then
	DOCKER_REPO=jex
fi

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
GIT_COMMIT="$(git rev-parse HEAD)"
BUILD_USER="$(whoami)"

if [ -d pkg/ ]; then
	rm -r pkg/
fi

if [ -d bin/ ]; then
	rm -r bin/
fi

docker pull $DOCKER_USER/buildenv:latest
docker run --rm  \
	-v $(pwd):/jex \
	-w /jex \
	$DOCKER_USER/buildenv:latest \
	gb build --ldflags "-X main.appver=$VERSION -X main.gitref=$GIT_COMMIT -X main.builtby=$BUILD_USER"
docker build --rm -t "$DOCKER_USER/$DOCKER_REPO:dev" .
docker push $DOCKER_USER/$DOCKER_REPO:dev
docker rmi $DOCKER_USER/$DOCKER_REPO:dev
