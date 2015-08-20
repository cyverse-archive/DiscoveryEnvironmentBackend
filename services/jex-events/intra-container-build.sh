#!/bin/sh

set -x
set -e

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
godep restore
go build --ldflags "-X main.appver=$VERSION -X main.gitref=$GIT_COMMIT -X main.builtby=$BUILD_USER" .
