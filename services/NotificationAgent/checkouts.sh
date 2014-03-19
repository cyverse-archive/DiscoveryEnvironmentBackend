#!/bin/bash

if [ ! -d checkouts ]; then
  mkdir checkouts
fi

make_link () {
  echo "Creating link to $1"
  ln -s ../../../libs/$1/ $1
}

pushd checkouts
make_link iplant-clojure-commons
make_link kameleon
popd
