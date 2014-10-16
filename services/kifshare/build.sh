#!/bin/bash
set -x

ITERATION=$1
USER=iplant
GROUP=iplant
BINNAME=kifshare
BUILDDIR=$BINNAME-build
BINDIR=/usr/local/lib/$BINNAME
LOGDIR=/var/log/$BINNAME
VERSION=$(cat version | sed -e 's/^ *//' -e 's/ *$//')
CONFDIR=/etc/kifshare
REPODIR=conf

node --version
grunt --version
npm --version
npm install
grunt build-resources

echo "Remove old build dir"
if [ -d "$BUILDDIR" ]; then
  rm -r $BUILDDIR
fi

echo "Create the build directory"
mkdir -p $BUILDDIR/$BINDIR

echo "Copying in the build directory, which contains the resources"
cp -r build/* ${BUILDDIR}/${BINDIR}

echo "Creating the log directory."
mkdir -p $BUILDDIR/$LOGDIR

mkdir -p $BUILDDIR/$CONFDIR
lein clean
lein deps
lein uberjar

cp target/$BINNAME-*-standalone.jar $BUILDDIR/$BINDIR
cp $REPODIR/* $BUILDDIR/$CONFDIR

fpm -s dir -t rpm --directories $LOGDIR -d java-1.7.0-openjdk --version $VERSION --iteration $ITERATION --epoch 0 --prefix / --name $BINNAME --verbose -C $BUILDDIR --rpm-user $USER --rpm-group $GROUP -f .
