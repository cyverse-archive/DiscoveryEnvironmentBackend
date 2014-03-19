#!/bin/sh

if [ -d checkouts ]; then
  pushd checkouts
  for i in $(ls); do
    cd $i
    echo ">>> Cleaning $i"
    lein clean
    echo ">>> Installing $i"
    lein install
    cd ..
  done
  popd
fi
