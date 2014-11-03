#!/bin/sh
set -x
set -e

ITERATION=$1
USER=iplant
GROUP=iplant

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
if [ -d "je-build" ]; then
  rm -r je-build
fi
mkdir -p je-build/usr/local/bin
mkdir -p je-build/var/log/jex-events
godep restore
go build .
cp jex-events je-build/usr/local/bin/
fpm -s dir -t rpm --directories /var/log/jex-events --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name jex-events --verbose -C je-build --rpm-user $USER --rpm-group $GROUP -f .
