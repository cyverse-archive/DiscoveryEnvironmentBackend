#!/bin/bash

condor_master
java -jar /home/condor/jex-standalone.jar "$@"
