#!/bin/bash -e

tar czvf metadata-db.tar.gz -C src/main --exclude '*~' .
