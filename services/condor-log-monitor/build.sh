#!/bin/sh

ITERATION=$1
USER=condor
GROUP=condor

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
if [ -d build ]; then
  rm -r build
fi
mkdir -p build/usr/local/bin
mkdir -p build/var/log/condor-log-monitor
godep restore
go build .
cp condor-log-monitor build/usr/local/bin/
fpm -s dir -t rpm --directories /var/log/condor-log-monitor --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name condor-log-monitor --verbose -C build --rpm-user $USER --rpm-group $GROUP -f .
