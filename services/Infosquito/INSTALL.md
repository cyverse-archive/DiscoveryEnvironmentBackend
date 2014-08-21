# Installing Infosquito

This file contains instructions for installing Infosquito and all of its
dependencies. Before beginning, please ensure that you have the appropriate YUM
repository enabled on the host or hosts where Infosquito will be installed.
Please contact Core Software with questions about enabling the YUM repository.

## Install infosquito

Infosquito is also packaged as an RPM, so installation is similar to the
installation of the other backend services for the DE.

### Installation

```
$ sudo yum install infosquito
```

### Configuration

Infosquito reads in its configuration from a file. By default, it will look for
the file at /etc/iplant/de/infosquito.properties, but you can override the
path by passing Infosquito the --config setting at start up.

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
