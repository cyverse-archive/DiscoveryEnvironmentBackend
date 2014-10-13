#!/bin/sh

BRANCH=$1
WORKINGDIR=$2
echo "BRANCH=$BRANCH"
echo "WORKINGIDR=$WORKINGDIR"

cd $WORKINGDIR
VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
USER=condor
GROUP=condor
git checkout $BRANCH
mkdir -p build/usr/local/bin
mkdir -p build/var/log/condor-log-monitor
godep restore
go build .
cp condor-log-monitor build/usr/local/bin/
fpm -s dir -t rpm --directories /var/log/condor-log-monitor --version $VERSION --epoch 0 --prefix / --name condor-log-monitor --verbose -C build --rpm-user $USER --rpm-group $GROUP -f .
