dewey
=====

An AMQP message based iRODS indexer for elasticsearch.

dewey listens to the `irods` AMQP topic exchange for iRODS change messages. The iRODS change
messages are described in the document
https://docs.google.com/document/d/126uSOX8VWfFyRub1Ibqiknf4QbQuDZLOCktoBLpgqsE/edit. For each
change message, dewey updates the elasticsearch `data` index with the corresponding change. The web
page https://github.com/iPlantCollaborativeOpenSource/proboscis/tree/master/config/mappings
describes `data` schema.

Usage
-----

dewey is a service that is distributed as an RPM. To install and start the service on any RedHat-
compatible OS, run these commands:

```
$ sudo yum install dewey
$ sudo /sbin/service dewey start
```

Dewet gets its configuration settings from a configuration file. The path
to the configuration file is given with the --config command-line setting.

Status Endpoint
---------------

Dewey has an extremely basic status endpoint. It's only real purpose right now is to confirm that Dewey is up and running. You can perform a GET request against the /status endpoint. Here's an example with curl:

    > curl http://not-a-real-dewey-host.org:5555/status
    {"status":"ok","service":"dewey"}
    
The port that dewey listens for status requests on is configurable with the 'dewey.status.listen-port' setting.

License
-------

See the file LICENSE.txt.
