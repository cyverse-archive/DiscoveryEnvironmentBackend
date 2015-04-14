#!/bin/bash

pushd ../../databases/de-database-schema/
./build.sh
popd
cp ../../databases/de-database-schema/database.tar.gz .
pushd ../../tools/facepalm/
#lein clean
#lein uberjar
docker run --rm -t -a stdout -a stderr -e "GIT_COMMIT=$(git rev-parse HEAD)" -v $(pwd):/build -v ~/.m2:/root/.m2 -w /build discoenv/buildenv lein uberjar
popd
cp ../../tools/facepalm/target/facepalm-standalone.jar .
docker build -t discoenv/de-db-loader .
