# Table of Contents

* [Installing and Configuring metadactyl-clj](#installing-and-configuring-metadactyl-clj)
    * [Primary Configuration](#primary-configuration)
    * [Logging Configuration](#logging-configuration)

# Installing and Configuring metadactyl-clj

metadactyl-clj is packaged as an RPM and published in iPlant's YUM repositories.
It can be installed using `yum install metadactyl` and upgraded using
`yum upgrade metadactyl`.

## Primary Configuration

metadactyl-clj gets its configuration settings from a configuration file. The path
to the configuration file is given with the --config command-line setting.

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
