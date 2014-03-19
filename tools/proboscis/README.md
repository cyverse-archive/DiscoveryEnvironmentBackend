# proboscis

A Clojure utility to initialize the ElasticSearch index for iPlant Collaborative's Data Commons. If
the index already exists, it will delete the index before initialization.

## Usage

```
proboscis -h host -p port
proboscis --help
```

## Arguments

Both of the arguments to proboscis are optional and have default values that will be used if no
other values is specified.

### -h --host

The host name or IP address of the machine where ElasticSearch is running. If not specified, this
argument defaults to `localhost`.

### -p --port

The port number that ElasticSearch is listening to. If not specified, this argument defaults to
`9200`.

### -? --help

Displays the a help message and exits.

## Bugs and Limitations

There are no known bugs in this utility. Please report any problems to the Core Software team.

## License

http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
