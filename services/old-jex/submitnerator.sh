#!/bin/bash

WORKDIR=$1
SUBMIT=$2

cd $WORKDIR
condor_submit $SUBMIT
