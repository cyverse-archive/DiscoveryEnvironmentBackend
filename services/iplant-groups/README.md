# iplant-groups 

A RESTful facade in front of [Grouper](http://www.internet2.edu/products-services/trust-identity-middleware/grouper/).

## Usage

To compile a jar file for use, run `lein do clean, uberjar`. This will produce `target/iplant-groups-standalone.jar`. To simply run a server for direct use, run `lein run`

Fishy listens on port 31310 rather than any configured port when run with lein-ring or a jar produced by lein-ring, rather than the methods listed above.

### Configuration

This program requires a configuration file to run, provided via the `--config` command-line option. A sample configuration:

```properties
# App settings
iplant-groups.app.listen-port     = 60000
# Grouper settings

iplant-groups.grouper.base-url    = http://some-grouper-server:5555/grouper-ws
iplant-groups.grouper.api-version = json/2.2.1
iplant-groups.grouper.username    = epinephelinae
iplant-groups.grouper.password    = really-big-fish
```

### Docker

This program includes a Dockerfile for use with docker. To use it, first build a jar with `lein do clean, uberjar`, and then run `docker build -t iplant-groups .` and `docker run --rm iplant-groups` to run the container. You will likely want to mount a configuration file when running the container with the `-v` option, and the container exposes port 60000 by default, so you should most likely configure this in your properties file.

## License

Copyright © 2015 iPlant Collaborative, Arizona Board of Regents

Distributed under the terms of the iPlant License:
http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
