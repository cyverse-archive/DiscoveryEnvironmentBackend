#!/bin/sh

for project in $(find . -name checkouts.sh -exec dirname '{}' \;); do
  pushd $project
  if [ -d ./checkouts ]; then
    rm -r ./checkouts
  fi

  ./checkouts.sh

  if [ -f ./install-checkouts.sh ]; then
    ./install-checkouts.sh
  fi
  popd
done
