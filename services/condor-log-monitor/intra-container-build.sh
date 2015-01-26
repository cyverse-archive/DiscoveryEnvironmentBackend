#!/bin/sh

set -x
set -e

godep restore
go build
