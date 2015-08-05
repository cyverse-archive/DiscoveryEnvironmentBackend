# metadata

The REST API for the Discovery Environment Metadata services.

Once running, endpoint documentation may be viewed by navigating a web browser to the server and
port this service is configured to run on, at the `/docs` endpoint. For example, if running the service with
`lein ring server`, then viewing http://localhost:60000/docs in a browser would display the documentation
of all available endpoints.

## Usage

### Configuration

This program requires a configuration file to run, passed via the `--config` command-line argument or found by default at `/etc/iplant/de/metadata.properties`. A sample configuration:

```properties
# Connection details.
metadata.app.listen-port = 60000

# Database settings.
metadata.db.driver      = org.postgresql.Driver
metadata.db.subprotocol = postgresql
metadata.db.host        = localhost
metadata.db.port        = 5432
metadata.db.name        = metadata
metadata.db.user        = de
metadata.db.password    = overwhelmingly-secret
```

### Run the application locally

`lein ring server`

### Packaging and running as standalone jar

```
lein do clean, uberjar
java -jar target/metadata-standalone.jar -c /path/to/configs
```

or 

```
java -cp target/metadata-standalone.jar:conf/test/:/path/to/configs/ metadata.core
```

### Run with Docker

 * Create a standalone jar file as specified above; it should be available at `target/metadata-standalone.jar`.
 * `docker build -t de-metadata .` from the top level of this project.
 * Ensure you have an accessible postgres database which has been initialized with the metadata-db tarball. In the below command, this database is running inside another Docker container named 'de-postgres'. When using Docker linking, linked containers can be used directly as hostnames in the configuration file.
 * `docker run --name de-metadata -p 3001:60000 -v ~/conf-files/metadata.properties:/home/iplant/metadata.properties --link de-postgres:de-postgres --rm de-metadata --config metadata.properties` (or, replace `--rm` with `-d` to daemonize to the background instead). This assumes your configuration file is present at `~/conf-files/metadata.properties`; correct as necessary.
 * Use the service at http://localhost:3001 or `http://$(boot2docker ip):3001/` for Boot2Docker users.
