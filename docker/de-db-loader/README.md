# DE DB Loader Docker Image

This image is used to populate a PostgreSQL instance running in a de-db container with the tables/constraints/data for the DE database.

DO NOT RUN THIS IN A PRODUCTION ENVIRONMENT!

The de-db and de-db-loader images are intended to make development of the DE databases a little easier. They are not intended to act as a production-ready replacement for your PostgreSQL deployment.

# Quickstart

Run a named instance of de-db as a daemon:

    docker run --name de-db -e POSTGRES_PASSWORD=notprod -d discoenv/de-db

Run a de-db-loader container, linking it to the de-db container you just started:

    docker run --rm --link de-db:postgres discoenv/de-db-loader:dev

When de-db-loader finishes, you should have an intialized instance of the DE database running in the de-db container.

# Resetting the database

To reset the database, first kill the existing instance:

    docker kill de-db

Then remove the cached container:

    docker rm de-db

Then you can rerun the de-db and de-db-loader containers as described in the Quickstart.

# Loading in a custom database tarball

To load in your own database build, create a database tarball and bind mount it into the de-db-loader container when you run it. That is, replace the second docker command from the Quickstart with this:

    docker run --rm --link de-db:postgres -v /path/to/database.tar.gz:/database.tar.gz discoenv/de-db-loader

The /path/to/database.tar.gz is the local path to your custom database, while the /database.tar.gz is the path to the tarball in the container.

# Using a custom facepalm build

To use your own uberjarred build of facepalm, create the uberjar and bind mount it into the de-db-loader container when you run it. That is, replace the second docker command from the Quickstart with this:

    docker run --rm --link de-db:postgres -v /path/to/facepalm-standalone.jar:/facepalm-standalone.jar discoenv/de-db-loader

The /path/to/facepalm-standalone.jar is the local path to your facepalm build, while the /facepalm-standalone.tar.gz is the path to the uberjar in the container.

# Updating the database

A custom script is provided in the de-db-loader image to assist with getting the database updated from a tarball. To run facepalm in update mode run the following command:

    docker run --rm --link de-db:postgres discoenv/de-db-loader /update-dev-database.sh

You will probably want to bind mount new versions of facepalm-standalone.jar and/or database.tar.gz into the container when doing this, otherwise it's a no-op as far as the database is concerned. See the above sections for more info.

# More info

de-db is a very small modification on top of the official Postgres image for docker. See https://registry.hub.docker.com/_/postgres/ for more info, including how to connect to the database with psql.
