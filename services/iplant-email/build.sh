#!/bin/sh
set -x
set -e

ITERATION=$1
USER=iplant
GROUP=iplant
BINNAME=iplant-email
BUILDDIR=$BINNAME-build
BINDIR=/usr/local/lib/$BINNAME
LOGDIR=/var/log/$BINNAME
CONFDIR=/etc/iplant-email
REPODIR=conf

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
if [ -d "$BUILDDIR" ]; then
  rm -r $BUILDDIR
fi
mkdir -p $BUILDDIR/$BINDIR
mkdir -p $BUILDDIR/$LOGDIR
mkdir -p $BUILDDIR/$CONFDIR
lein clean
lein deps
lein uberjar
cp target/$BINNAME-standalone.jar $BUILDDIR/$BINDIR
cp $REPODIR/* $BUILDDIR/$CONFDIR
fpm -s dir -t rpm --directories $LOGDIR -d java-1.7.0-openjdk --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name $BINNAME --verbose -C $BUILDDIR --rpm-user $USER --rpm-group $GROUP -f .
