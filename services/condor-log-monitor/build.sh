#!/bin/sh
set -x
set -e

ITERATION=$1
USER=condor
GROUP=condor

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
if [ -d "build" ]; then
  rm -r clm-build
fi
mkdir -p clm-build/usr/local/bin
mkdir -p clm-build/var/log/condor-log-monitor
godep restore
go build .
ls -l build
cp condor-log-monitor clm-build/usr/local/bin/
fpm -s dir -t rpm --directories /var/log/condor-log-monitor --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name condor-log-monitor --verbose -C clm-build --rpm-user $USER --rpm-group $GROUP -f .
