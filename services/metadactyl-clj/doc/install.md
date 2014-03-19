# Table of Contents

* [Installing and Configuring metadactyl-clj](#installing-and-configuring-metadactyl-clj)
    * [Primary Configuration](#primary-configuration)
    * [Zookeeper Connection Information](#zookeeper-connection-information)
    * [Logging Configuration](#logging-configuration)

# Installing and Configuring metadactyl-clj

metadactyl-clj is packaged as an RPM and published in iPlant's YUM repositories.
It can be installed using `yum install metadactyl` and upgraded using
`yum upgrade metadactyl`.

## Primary Configuration

metadactyl-clj gets most of its configuration settings from Apache Zookeeper.
These configuration setting are uploaded to Zookeeper using Clavin, a command-
line tool maintained by iPlant that allows configuration properties and access
control lists to easily be uploaded to Zookeeper.  Please see the Clavin
documentation for information about how to upload configuration settings.
Here's an example configuration file:

```properties
# Connection details.
metadactyl.app.listen-port = 65007

# Database settings.
metadactyl.db.driver      = org.postgresql.Driver
metadactyl.db.subprotocol = postgresql
metadactyl.db.host        = localhost
metadactyl.db.port        = 65001
metadactyl.db.name        = some-db
metadactyl.db.user        = some-user
metadactyl.db.password    = some-password

# Hibernate resource definition files.
metadactyl.hibernate.resources = template-mapping.hbm.xml, \
                                 notifications.hbm.xml, \
                                 workflow.hbm.xml

# Java packages containing classes with JPA Annotations.
metadactyl.hibernate.packages = org.iplantc.persistence.dto.step, \
                                org.iplantc.persistence.dto.transformation, \
                                org.iplantc.persistence.dto.data, \
                                org.iplantc.persistence.dto.workspace, \
                                org.iplantc.persistence.dto.user, \
                                org.iplantc.persistence.dto.components, \
                                org.iplantc.persistence.dto.listing, \
                                org.iplantc.persistence.dto.refgenomes, \
                                org.iplantc.workflow.core

# The Hibernate dialect to use.
metadactyl.hibernate.dialect = org.hibernate.dialect.PostgreSQLDialect

# OSM connection settings.
metadactyl.osm.base-url           = http://localhost:65009
metadactyl.osm.connection-timeout = 5000
metadactyl.osm.encoding           = UTF-8
metadactyl.osm.jobs-bucket        = jobs
metadactyl.osm.job-request-bucket = job_requests

# JEX connection settings.
metadactyl.jex.base-url = http://localhost:65006

# Workspace app group names.
metadactyl.workspace.root-app-group            = Workspace
metadactyl.workspace.default-app-groups        = ["Apps under development","Favorite Apps"]
metadactyl.workspace.dev-app-group-index       = 0
metadactyl.workspace.favorites-app-group-index = 1

# The domain name to append to the user id to get the fully qualified user id.
metadactyl.uid.domain = iplantcollaborative.org
```

Generally, the database and service connection settings will have to be
updated for each deployment.

## Zookeeper Connection Information

One piece of information that can't be stored in Zookeeper is the information
required to connect to Zookeeper.  For metadactyl-clj and most other iPlant
services, this information is stored in a single file:
`/etc/iplant-services/zkhosts.properties`.  This file is automatically
installed when the iplant-service-configs RPM is installed.  You may have to
modify this file so that it points to the correct hosts.

## Logging Configuration

The logging settings are stored in `/etc/metadactyl/log4j.properties`.  The file
looks like this by default:

```properties
log4j.rootLogger=WARN, A

# Uncomment these lines to enable debugging for iPlant classes.
# log4j.category.org.iplantc=DEBUG, A
# log4j.additivity.org.iplantc=false

# Uncomment these lines to enable debugging in metadactyl-clj itself.
# log4j.category.metadactyl=DEBUG, A
# log4j.additivity.metadactyl=false

# Uncomment these lines to enable debugging in iPlant Clojure Commons.
# log4j.category.clojure-commons=DEBUG, A
# log4j.additivity.clojure-commons=false

# Either comment these lines out or change the appender to B when running
# metadactyl-clj in the foreground.
log4j.logger.JsonLogger=debug, JSON
log4j.additivity.JsonLogger=false

# Use this appender for logging JSON when running metadactyl-clj in the background.
log4j.appender.JSON=org.apache.log4j.RollingFileAppender
log4j.appender.JSON.File=/var/log/metadactyl/json.log
log4j.appender.JSON.layout=org.apache.log4j.PatternLayout
log4j.appender.JSON.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.JSON.MaxFileSize=10MB
log4j.appender.JSON.MaxBackupIndex=1

# Use this appender when running metadactyl-clj in the background.
log4j.appender.A=org.apache.log4j.RollingFileAppender
log4j.appender.A.File=/var/log/metadactyl/metadactyl.log
log4j.appender.A.layout=org.apache.log4j.PatternLayout
log4j.appender.A.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.A.MaxFileSize=10MB
log4j.appender.A.MaxBackupIndex=1
```

The most useful configuration change here is to enable debugging for iPlant
classes, which can be done by uncommenting two lines.  In rare cases, it may
be helpful to enable debugging in metadactyl-clj and iPlant Clojure Commons.
Most of the logic in metadactyl-clj is implemented in Java classes that are
underneath the org.iplantc package, however, so enabling debugging for those
classes will be the most helpful.

See the [log4j documentation](http://logging.apache.org/log4j/1.2/manual.html)
for additional logging configuration instructions.
