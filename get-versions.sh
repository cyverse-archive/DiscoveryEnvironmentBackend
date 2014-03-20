#!/bin/sh

find . -name project.clj -exec head -n 1 '{}' \;
