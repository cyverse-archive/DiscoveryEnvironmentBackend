# Services documentation

This directory contains the projects for each of the Discovery Environment's backend services. Most of the services are written in Clojure, the exceptions being condor-log-monitor and jex-events.

If you're building locally ignore the docker-build.sh and build.sh files. Those are intended for our Jenkins build system and are a bit too unwieldy to be manually run.

## Building Clojure services

Periodically -- as in once a day or so -- run the following command from the top-level of the backend checkout (in other words, one directory up from this file):

    > docker run --rm -v ~/.m2/:/root/.m2/ -v $(pwd):/build -w /build discoenv/buildenv lein exec build-all.clj lein-plugins libs

That will build the latest version of all of the libraries for the backend and place them into your local .m2 directory. As annoying as that is to type, it's still less annoying than trying to get a full development environment set up on your local box.

To build a new version of a Clojure service, run something like the following inside the project directory for the service. We'll use anon-files as a concrete example:

    > docker run --rm -v ~/.m2/:/root/.m2/ -v $(pwd):/build -w /build discoenv/buildenv lein uberjar
    > docker build -t discoenv/anon-files:dev .

Substitute the project name -- in lowercase -- for "anon-files" in the "docker build" command.

The build of the uberjar is separate from the build of the container image to keep the size of the container image a bit more reasonable.

## Building Go services

The Go services more self-contained, so you don't have to worry about building the libs each day like you do with the Clojure projects.

To build a new version of a Go service, run something like the following inside the project directory for the service. We'll use condor-log-monitor as a concrete example:

    > docker run --rm -t -v $(pwd):/condor-log-monitor -v $(pwd)/intra-container-build.sh:/bin/intra-container-build.sh -w /condor-log-monitor discoenv/clm-builder
    > docker build -t discoenv/condor-log-monitor:dev .

Substitute "jex-events" (without the quotes) for all instances of "condor-log-monitor" to build jex-events. It uses the clm-builder container image as well.
