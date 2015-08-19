# anon-files

A Discovery Environment service that serves up files that have been shared with the anonymous user in iRODS.

## Build

Periodically -- as in once a day or so -- run the following command from the top-level of the backend checkout:

    > docker run --rm -it -v ~/.m2/:/root/.m2/ -v $(pwd):/build -w /build discoenv/buildenv lein exec build-all.clj lein-plugins libs

That will build the latest version of all of the libraries for the backend and place them into your local .m2 directory. As annoying as that is to type, it's still less annoying than trying to get a full development environment set up on your local box.

To build a new version of anon-files run the following inside the services/anon-files/ directory of the checkout (which contains this file):

    > docker run --rm -v ~/.m2/:/root/.m2/ -v $(pwd):/build -w /build discoenv/buildenv lein uberjar
    > docker build -t discoenv/anon-files:dev .

The build of the uberjar is separate from the build of the container image to keep the size of the container image a bit more reasonable.

## Configure

anon-files's configuration file must be in the properties file format. Here's an unconfigured anon-files properties file:

    irods-host =
    irods-port =
    irods-user =
    irods-password =
    irods-zone =
    irods-home =
    anon-user = anonymous
    port = 60000
    log-file =
    
The Dockerfile is expecting anon-files to be listening on port 60000, so it's not recommended to place something different in the config file.

Here are the command-line options:

    A service that serves up files shared with the iRODS anonymous user.

    Usage: anon-files [options]

    Options:
      -p, --port PORT                                       Port number
      -c, --config PATH      /etc/iplant/de/anon-files.edn  Path to the config file
      -v, --version                                         Print out the version number.
      -h, --help

## Run

    docker run -P -d --name anon-files -v /path/to/config:/etc/iplant/de/anon-files.properties discoenv/anon-files

Use *docker ps* to see which random port anon-files is listening on.

## Downloading files

anon-files supports downloading files either all at once or by byte ranges. The caller must already know the path to the file inside iRODS; the path in iRODS corresponds to the path in the URI.

anon-files does not currently support request pre-conditions or multiple byte ranges per request.

If the lower bound on a request is greater than the file's size, anon-files will return a 416 status code as defined in http://tools.ietf.org/html/rfc7233.

If the upper bound on a range request is greater than the file size, then the returned data will go up to the end of the file and have a status of 206.

If a range request consists of a single negative value, then it is used as a negative index into the content and the range is interpreted as being from that point to the end of the content.

    curl -H "Range: bytes=-10" http://localhost:8080/path/to/file

If a range request consists of a single positive value, then the range header is ignored and the entire content is downloaded. A single value is not enough information to determine what is wanted from the client, but the RFC implies that it shouldn't be treated as an error (unless I'm missing something, which is likely). To download a single byte from the content, perform a request like the following:

    curl -H "Range: bytes=10-10" http://localhost:8080/path/to/file

A successful ranged request will return the requested byte range and have a 206 status code. A requested range that is unsatisfiable (for instance, the lower bound is higher than the file size) will have a status code of 416.

Byte range requests will have the following HTTP headers in the response:
* Content-Range (see http://tools.ietf.org/html/rfc7233 for more info)
* Content-Length (see http://tools.ietf.org/html/rfc7233 and http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html for more info)
* Accept-Ranges (always set to bytes)
* Cache-Control (this will always be set to no-cache)
* ETag (we're using a weak ETag based on the last modified date)
* Expires (always set to 0)
* Vary (always set to *)
* Content-Location (the path to the file in iRODS)

## License

See [LICENSE](LICENSE)
