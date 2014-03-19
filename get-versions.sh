#!/bin/sh

for toplevel in services libs; do
  pushd $toplevel 2>&1 > /dev/null
  for d in $(ls); do
    pushd $d 2>&1 > /dev/null
    if [ -f project.clj ]; then
      head -n 1 project.clj
    fi
    popd 2>&1 > /dev/null
  done
  popd 2>&1 > /dev/null
done
