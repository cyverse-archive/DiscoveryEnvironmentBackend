#!/bin/sh
set -x
set -e

ITERATION=$1
USER=iplant
GROUP=iplant

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
if [ -d "jex-build" ]; then
  rm -r jex-build
fi
mkdir -p jex-build/usr/local/bin
mkdir -p jex-build/var/log/jex-events
godep restore
go build .
cp jex-events jex-build/usr/local/bin/
fpm -s dir -t rpm --directories /var/log/jex-events --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name jex-events --verbose -C jex-build --rpm-user $USER --rpm-group $GROUP -f .
