#!/bin/sh

JEXDB="postgres://de:notprod@$JEXDB_PORT_5432_TCP_ADDR:5432/jex?sslmode=disable"
export JEXDB
gb test -test.v
