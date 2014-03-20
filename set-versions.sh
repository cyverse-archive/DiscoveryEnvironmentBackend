#!/bin/sh

if [ $# -eq 0 ]; then
  echo "USAGE: ./set-versions.sh <version>"
  exit 1
fi

echo "NOTE: This script requires the lein-set-version plugin for Leiningen."
echo "I don't know if you have it already, but if you need it go to: "
echo "https://github.com/pallet/lein-set-version"

for project in $(find . -name project.clj -exec dirname '{}' \;); do
	pushd $project 2>&1 > /dev/null
	cljproj=$(basename $project)
	if [ "$cljproj" == "Donkey" ]; then
		echo ">>> $cljproj: Setting version to $1-SNAPSHOT"
		lein set-version $1-SNAPSHOT
	else
		echo ">>> $cljproj: Setting version to $1"
		lein set-version $1
	fi
	popd 2>&1 > /dev/null
done

