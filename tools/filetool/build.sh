#!/bin/sh
set -x
set -e

ITERATION=$1
USER=iplant
GROUP=iplant
BINNAME=porklock
BUILDDIR=$BINNAME-build
BINDIR=/usr/local/lib/$BINNAME
LOGDIR=/var/log/$BINNAME
PATHBIN=/usr/local/bin

VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
if [ -d "$BUILDDIR" ]; then
  rm -r $BUILDDIR
fi
mkdir -p $BUILDDIR/$BINDIR
mkdir -p $BUILDDIR/$LOGDIR
mkdir -p $BUILDDIR/$PATHBIN
lein clean
lein deps
lein uberjar
cp curl_wrapper.pl $BUILDDIR/$PATHBIN
cp porklock $BUILDDIR/$PATHBIN
cp target/$BINNAME-standalone.jar $BUILDDIR/$BINDIR
chmod 755 $BUILDDIR/$BINDIR/curl_wrapper.pl
chmod 755 $BUILDDIR/$BINDIR/porklock
fpm -s dir -t rpm --directories $LOGDIR -d java-1.7.0-openjdk --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name $BINNAME --verbose -C $BUILDDIR --rpm-user $USER --rpm-group $GROUP -f .
