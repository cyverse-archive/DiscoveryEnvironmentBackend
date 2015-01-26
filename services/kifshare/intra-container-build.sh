#!/bin/sh

set -e
set -x

npm --version
grunt --version

npm install
grunt build-resources
lein uberjar

