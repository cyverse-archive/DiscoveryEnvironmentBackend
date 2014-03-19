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
make_link mescal
make_link kameleon
make_link heuristomancer
make_link clj-icat-direct
popd
