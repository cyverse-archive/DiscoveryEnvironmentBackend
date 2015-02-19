# metadactyl-clj

metadactyl-clj is a platform for hosting App Services for the Discovery Environment web application.

Once running, endpoint documentation may be viewed by navigating a web browser to the server and
port this service is configured to run on. For example, if the service were configured as described
in the [Primary Configuration docs](doc/install.md#primary-configuration), then viewing
http://localhost:65007 in a browser would display the documentation of all available endpoints.

## Documentation Links

* [Installation and Configuration](doc/install.md)
* [Security](doc/security.md)
* [Errors](doc/errors.md)


## Obsolete Documentation Links (to be removed)
* [App Administration Services](doc/endpoints/app-metadata/admin.md)
* [Updated App Administration Services](doc/endpoints/app-metadata/updated-admin.md)


## Unit Testing And Development

You'll need to have Docker installed for this stuff to work.

The test.sh script uses the discoenv/de-db and discoenv/de-db-loader images to
get a PostgreSQL container running locally (listening on local port 5432) and
then runs the Metadactyl's unit tests from within a Clojure container created
with the official Docker image.

test.sh will kill and remove any containers named 'de-db' when it first starts
up. If you need to manually kill and remove the de-db container:

    docker kill de-db
    docker rm de-db

test.sh will also run 'boot2docker shellinit' if you're running it on OS X.

The repl.sh script is a variation of test.sh which will start up an interactive
REPL instead of running the unit tests.

The psql.sh script will use the official PostgreSQL Docker image to create a
container that links to the de-db container and fires up psql. The password
is 'notprod' (without the quotes).

The file test.properties is a config file for Metadactyl that is set up to
assume that everything will be running  locally. To get Metadactyl up and
running with de-db as the database, fire off test.sh and then run the following:

    lein run -- --config test.properties
