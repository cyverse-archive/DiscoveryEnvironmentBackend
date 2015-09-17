#!/bin/sh
set -x
set -e

if [ -z "$DOCKER_USER" ]; then
	DOCKER_USER=discoenv
fi

if [ -z "$DOCKER_REPO" ]; then
	DOCKER_REPO=jex-events
fi

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')

docker pull $DOCKER_USER/buildenv:latest
docker run --rm  \
	-e "VERSION=$VERISON" \
	-e "GIT_COMMIT=$(git rev-parse HEAD)" \
	-e "BUILD_USER=$(whoami)" \
	-v $(pwd):/jex-events \
	-w /jex-events \
	$DOCKER_USER/buildenv:latest \
	gb build --ldflags "-X main.appver=$VERSION -X main.gitref=$GIT_COMMIT -X main.builtby=$BUILD_USER"
docker build --rm -t "$DOCKER_USER/$DOCKER_REPO:dev" .
docker push $DOCKER_USER/$DOCKER_REPO:dev
docker rmi $DOCKER_USER/$DOCKER_REPO:dev
