#!/bin/sh

pushd ../../databases/de-database-schema/
./build.sh
popd
cp ../../databases/de-database-schema/database.tar.gz .
pushd ../../tools/facepalm/
lein clean
lein uberjar
popd
cp ../../tools/facepalm/target/facepalm-standalone.jar .
docker build -t discoenv/de-db-loader .
