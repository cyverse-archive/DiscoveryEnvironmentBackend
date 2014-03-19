#!/bin/bash -e

tar czvf database.tar.gz -C src/main --exclude '*~' .
