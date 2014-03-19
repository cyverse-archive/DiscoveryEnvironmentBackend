# Installing Infosquito

This file contains instructions for installing Infosquito and all of its
dependencies. Before beginning, please ensure that you have the appropriate YUM
repository enabled on the host or hosts where Infosquito will be installed.
Please contact Core Software with questions about enabling the YUM repository.

## Overview

* [Install elasticsearch](#install-elasticsearch)
* [Install infosquito](#install-infosquito)

## Install Elasticsearch

An RPM for elasticsearch is available in iPlant's YUM repositories. This RPM
provides the standard elasticsearch installation along with an initd script that
can be used to start and stop elasticsearch.

Note that the init script uses Clavin to obtain configuration settings from
Zookeeper. Clavin will automatically be installed when elasticsearch is
installed using this RPM package.

### Installation

```
$ sudo yum install elasticsearch
```

### Configuration

The only configurable setting for this elasticsearch installation is the port
number that elasticsearch listens to for incoming connections.

```
elasticsearch.app.listen-port = 9200
```

Note that the default configuration is a single node cluster, which doesn't
support multiple shards or any shard replication. Some manual configuration will
be necessary to create an actual cluster. When elasticsearch is installed using
the RPM provided by iPlant, its configuration files are located in
`/usr/share/elasticsearch/config`. Please see the
[elasticsearch documentation](http://www.elasticsearch.org/guide/) for more
information about configuring an elasticsearch cluster.

### Index Initialization

We've created a utility called
[proboscis](https://github.com/iPlantCollaborativeOpenSource/proboscis) to help
with the index initialization. In most cases, the index can be initialized using
the following command:

```
$ proboscis -h hostname
```

Other customization options are available, however. For more information, plase
see the documentation at the link above.

### Verifying Index Initialization

To verify that the index has been initialized correctly, you can use
elasticsearch's `_mappings` endpoint. Assuming that you used the default index
name and port number when init:

```
$ curl -s http://hostname:31338/iplant/_mapping | python -mjson.tool
{
    "iplant": {
        "file": {
            "properties": {
                "acl": {
                    "properties": {
                        "name": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        },
                        "permission": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        },
                        "zone": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        }
                    }
                },
                "create_date": {
                    "format": "dateOptionalTime",
                    "type": "date"
                },
                "creator": {
                    "properties": {
                        "name": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        },
                        "zone": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        }
                    }
                },
                "modify_date": {
                    "format": "dateOptionalTime",
                    "type": "date"
                },
                "name": {
                    "analyzer": "irods_entity",
                    "type": "string"
                },
                "parent_path": {
                    "analyzer": "irods_path",
                    "type": "string"
                }
            }
        },
        "folder": {
            "properties": {
                "acl": {
                    "properties": {
                        "name": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        },
                        "permission": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        },
                        "zone": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        }
                    }
                },
                "create_date": {
                    "format": "dateOptionalTime",
                    "type": "date"
                },
                "creator": {
                    "properties": {
                        "name": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        },
                        "zone": {
                            "index": "not_analyzed",
                            "index_options": "docs",
                            "omit_norms": true,
                            "type": "string"
                        }
                    }
                },
                "modify_date": {
                    "format": "dateOptionalTime",
                    "type": "date"
                },
                "name": {
                    "analyzer": "irods_entity",
                    "type": "string"
                },
                "parent_path": {
                    "analyzer": "irods_path",
                    "type": "string"
                }
            }
        }
    }
}
```

The result should look similar to the result included here.

## Install infosquito

Infosquito is also packaged as an RPM, so installation is similar to the
installation of the other backend services for the DE.

### Installation

```
$ sudo yum install infosquito
```

### Configulon

```properties
# ElasticSearch Settings
infosquito.es.host = somehost.example.org
infosquito.es.port = 5555

# ICAT Database Connection Settings
infosquito.icat.host = someotherhost.example.org
infosquito.icat.port = 4444
infosquito.icat.user = someuser
infosquito.icat.pass = somepassword
infosquito.icat.db   = somedatabase

# Indexing Options
infosquito.base.collection = /path/to/root

# AMQP Settings
infosquito.amqp.host           = yetanotherhost.example.org
infosquito.amqp.port           = 3333
infosquito.amqp.user           = someotheruser
infosquito.amqp.pass           = someotherpassword
infosquito.amqp.reindex-queue  = foo.bar
```

### Publishing a Sync Task

TODO: update this when the ability to add a new sync task has been added.

### Verifying Index Entries

It will take a while for index entries to appear for all files, so it may be
necessary to wait for a while before performing this step. To verify that a file
or folder has been indexed correctly, first pick a file or folder to check. For
the first example, I'm going to look at the folder `/iplant/home/ipctest`.

The first step is to examine the folder using the iRODS i-commands. You'll want
to use `ils -A` to display the folder permissions:

```
$ ils -A /iplant/home/ipctest | head -3
/iplant/home/ipctest:
        ACL - ipcservices#iplant:own   admin2#iplant:own   ipc_admin#iplant:own   ipctest#iplant:own   g:rodsadmin#iplant:own
        Inheritance - Disabled
```

The next step is to check to see if the path has been indexed. The easiest way
to do that is to use elasticsearch's GET API call. The pattern for the URL is
always `http://hostname:port/index/mapping/id` where the `index` is the name of
the index being used, mapping is the name of the mapping that should contain the
item and `id` is the item identifier. The name of the index is `iplant` by
default. The name of the mapping is either `file` or `folder` depending on the
typ of the item. The identifier is always a url-encoded version of the full path
to the file or folder.

```
$ curl -s http://localhost:9200/iplant/folder/%2fiplant%2fhome%2fipctest | python -mjson.tool
{
    "_id": "/iplant/home/ipctest",
    "_index": "iplant",
    "_source": {
        "acl": [
            {
                "name": "ipctest",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "ipc_admin",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "ipcservices",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "admin2",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "rodsadmin",
                "permission": "own",
                "zone": "iplant"
            }
        ],
        "create_date": 1302807084000,
        "creator": {
            "name": "ipctest",
            "zone": "iplant"
        },
        "modify_date": 1347212562000,
        "name": "ipctest",
        "parent_path": "/iplant/home"
    },
    "_type": "folder",
    "_version": 1,
    "exists": true
}
```

The goal here is to verify that the file is indexed and that the permissions
reported in the index are the same as the permissions reported by `ils`.  In
this case, the permissions do, in fact, match. It's important to ensure that the
permissons haven't changed since the file was indexed.

Verifying that a file was indexed correctly is similar. First, check list the
file along with its permissions using `ils`:

```
$ ils -A /iplant/home/ipctest/acer.nex
  /iplant/home/ipctest/acer.nex
        ACL - rodsadmin#iplant:own   ipcservices#iplant:own   ipc_admin#iplant:own   ipctest#iplant:own
```

Next, check the index:

```
$ curl -s http://localhost:9200/iplant/file/%2fiplant%2fhome%2fipctest%2facer.nex | python -mjson.tool
{
    "_id": "/iplant/home/ipctest/acer.nex",
    "_index": "iplant",
    "_source": {
        "acl": [
            {
                "name": "ipc_admin",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "ipcservices",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "ipctest",
                "permission": "own",
                "zone": "iplant"
            },
            {
                "name": "rodsadmin",
                "permission": "own",
                "zone": "iplant"
            }
        ],
        "create_date": 1317073972000,
        "creator": {
            "name": "de-irods",
            "zone": ""
        },
        "modify_date": 1320767629000,
        "name": "acer.nex",
        "parent_path": "/iplant/home/ipctest"
    },
    "_type": "file",
    "_version": 1,
    "exists": true
}
```

Once again, the entry appears to be indexed correctly.
