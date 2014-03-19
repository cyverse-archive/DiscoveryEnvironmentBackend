#!/bin/sh

if [ $# -eq 0 ]; then
  echo "USAGE: ./set-versions.sh <version>"
  exit 1
fi

echo "NOTE: This script requires the lein-set-version plugin for Leiningen."
echo "I don't know if you have it already, but if you need it go to: "
echo "https://github.com/pallet/lein-set-version"

for toplevel in services libs; do
  pushd $toplevel 2>&1 > /dev/null
  for d in $(ls); do
    pushd $d 2>&1 > /dev/null
    if [ -f project.clj ]; then
      if [ "$d" == "Donkey" ]; then
        echo ">>> $d: Setting version to $1-SNAPSHOT"
        lein set-version $1-SNAPSHOT
      else
        echo ">>> $d: Setting version to $1"
        lein set-version $1
      fi
    fi
    popd 2>&1 > /dev/null
  done
  popd 2>&1 > /dev/null
done
