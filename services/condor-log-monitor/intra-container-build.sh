#!/bin/bash

set -x
set -e

godep restore
go build
