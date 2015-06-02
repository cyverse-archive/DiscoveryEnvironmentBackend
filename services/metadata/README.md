# metadata

The REST API for the Discovery Environment Metadata services.

Once running, endpoint documentation may be viewed by navigating a web browser to the server and
port this service is configured to run on. For example, if running the service with
`lein ring server`, then viewing http://localhost:60000 in a browser would display the documentation
of all available endpoints.

## Usage

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
