# DE DB Loader Docker Image

This image is used to populate a PostgreSQL instance running in a de-db container with the tables/constraints/data for the DE database.

DO NOT RUN THIS IN A PRODUCTION ENVIRONMENT!

The de-db and de-db-loader images are intended to make development of the DE databases a little easier. They are not intended to act as a production-ready replacement for your PostgreSQL deployment.

# Usage

Run a named instance of de-db as a daemon:

    docker run --name de-db -e POSTGRES_PASSWORD=notprod -d discoenv/de-db

Run a de-db-loader container, linking it to the de-db container you just started:

    docker run --rm --link de-db:postgres discoenv/de-db-loader

When de-db-loader finishes, you should have an intialized instance of the DE database running in the de-db container.

# More info

de-db is a very small modification on top of the official Postgres image for docker. See https://registry.hub.docker.com/_/postgres/ for more info, including how to connect to the database with psql.