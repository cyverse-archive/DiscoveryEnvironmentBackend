# fishy

A RESTful facade in front of [Grouper](http://www.internet2.edu/products-services/trust-identity-middleware/grouper/).

## Usage

To compile a jar file for use, run `lein do clean, ring uberjar`. This will produce `target/fishy-standalone.jar`.

Fishy listens on port 31310

### Configuration

This program requires a configuration file called `fishy.properties` to run, found by searching `/etc/iplant/de`, the current working directory, and the classpath. A sample configuration:

```properties
# Grouper settings

fishy.grouper.base-url    = http://some-grouper-server:5555/grouperWs
fishy.grouper.api-version = v_2_1_000
fishy.grouper.username    = epinephelinae
fishy.grouper.password    = really-big-fish
```

## License

Copyright © 2015 iPlant Collaborative, Arizona Board of Regents

Distributed under the terms of the iPlant License:
http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
