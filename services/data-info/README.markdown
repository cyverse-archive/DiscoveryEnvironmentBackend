# data-info

data-info is a RESTful frontend for getting information about and manipulating
information in an iRODS data store.

## Building and running

data-info can be built with Leiningen: `lein do clean, uberjar`, and then run as a standard jar file `target/data-info-standalone.jar`. For development, you may also use `lein run` to compile and run at once.

## Configuration

data-info uses a properties-style configuration file, passed in via the `--config` command-line option or found by default at `/etc/iplant/de/data-info.properties`. An example configuration file:

```properties
data-info.anon-files-base-url        = https://example.org/anon-files/
data-info.anon-user                  = anonymous
data-info.bad-chars                  = \u0060\u0027\u000A\u0009
data-info.community-data             = /iplant/home/shared
data-info.copy-key                   = copy-from
data-info.data-threshold             = 8000
data-info.kifshare-download-template = \{\{url\}\}/d/\{\{ticket-id\}\}/\{\{filename\}\}
data-info.kifshare-external-url      = http://example.org/foo
data-info.max-paths-in-request       = 1000
data-info.metadata.base-url          = http://example.org:31331
data-info.perms-filter               = rodsadmin_acl,rodsBoot,rodsadmin,admin_proxy
data-info.port                       = 60000
data-info.preview-size               = 8000

# ICAT configuration
data-info.icat.host           = irods.example.org
data-info.icat.port           = 31398
data-info.icat.user           = irods
data-info.icat.password       = rods-and-cones
data-info.icat.db             = ICAT

# iRODS configuration
data-info.irods.host          = irods.example.org
data-info.irods.port          = 1247
data-info.irods.user          = rods
data-info.irods.password      = rods-and-cones
data-info.irods.home          = /iplant/home
data-info.irods.zone          = iplant
data-info.irods.resc          =
data-info.irods.max-retries   = 10
data-info.irods.retry-sleep   = 1000
data-info.irods.use-trash     = true
data-info.irods.admin-users   = rods,rodsadmin_acl,rodsBoot,rodsadmin,admin_proxy

# file typing configuration
data-info.type-detect.type-attribute       = ipc-filetype
data-info.type-detect.filetype-script      = /usr/local/bin/guess-2.pl
data-info.type-detect.filetype-read-amount = 1024
```
