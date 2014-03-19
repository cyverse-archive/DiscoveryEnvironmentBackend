#!/bin/sh

for toplevel in services libs; do
  pushd $toplevel
  for d in $(ls); do
    pushd $d
    if [ -f ./checkouts.sh ]; then
      ./checkouts.sh
    fi

    if [ -f ./install-checkouts.sh ]; then
      ./install-checkouts.sh
    fi
    popd
  done
  popd
done
