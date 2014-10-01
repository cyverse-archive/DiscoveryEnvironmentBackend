# Installing and Configuring data-info

data-info is packaged as an RPM and published in iPlant's YUM repositories.  It
can be installed using `yum install data-info` and upgraded using `yum upgrade
data-info`.

## Primary Configuration

data-info reads in its configuration from a file. By default, it will look for
the file at /etc/iplant/de/data-info.properties, but you can override the
path by passing data-info the --config setting at start up.

Here's an example configuration file:

```properties
# Connection details.
data-info.app.listen-port = 65002

# Environment information.
data-info.app.environment-name = example

# iPlant Email service connection settings.
data-info.email.base-url          = http://localhost:65003
data-info.email.tool-request-dest = somebody@iplantcollaborative.org
data-info.email.tool-request-src  = nobody@iplantcollaborative.org

# Metadactyl connection settings
data-info.metadactyl.base-url             = http://localhost:65007/secured
data-info.metadactyl.unprotected-base-url = http://localhost:65007

# Notification agent connection settings.
data-info.notificationagent.base-url = http://localhost:65011

# CAS Settings
data-info.cas.cas-server  = https://cas-server.iplantcollaborative.org/cas/
data-info.cas.server-name = http://localhost:65002

# The domain name to append to the user id to get the fully qualified user id.
data-info.uid.domain = iplantcollaborative.org

# User information lookup settings.
data-info.userinfo.base-url             = https://localhost/api/v1
data-info.userinfo.client-key           = some-client-key
data-info.userinfo.password             = some-client-password
data-info.userinfo.default-search-limit = 50

# Nibblonian connection settings
data-info.nibblonian.base-url = http://localhost:65010/

# JEX connection settings
data-info.jex.base-url = http://localhost:65006/

# Scruffian connection settings
data-info.scruffian.base-url = http://localhost:65013/

# Tree viewer settings
data-info.tree-viewer.base-url              = http://localhost/parseTree

# Elastic Search settings
data-info.infosquito.es-url = http://services-2.iplantcollaborative.org:31338
```

Generally, the service connection settings will have to be updated for each
deployment.

## Zookeeper Connection Information

One piece of information that can't be stored in Zookeeper is the information
required to connect to Zookeeper.  For data-info and most other iPlant services,
this information is stored in a single file:
`/etc/iplant-services/zkhosts.properties`.  This file is automatically
installed when the iplant-service-configs RPM is installed.  You may have to
modify this file so that it points to the correct hosts.

## Logging Configuration

The logging settings are stored in `/etc/data-info/log4j.properties`.  The file
looks like this by default:

```properties
log4j.rootLogger=WARN, A

# Uncomment these lines to enable debugging in data-info itself.
# log4j.category.data-info=DEBUG, A
# log4j.additivity.data-info=false

# Uncomment these lines to enable debugging in iPlant Clojure Commons.
# log4j.category.clojure-commons=DEBUG, A
# log4j.additivity.clojure-commons=false

# Either comment these lines out or change the appender to B when running
# data-info in the foreground.
log4j.logger.JsonLogger=debug, JSON
log4j.additivity.JsonLogger=false

# Use this appender for logging JSON when running data-info in the background.
log4j.appender.JSON=org.apache.log4j.RollingFileAppender
log4j.appender.JSON.File=/var/log/data-info/json.log
log4j.appender.JSON.layout=org.apache.log4j.PatternLayout
log4j.appender.JSON.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.JSON.MaxFileSize=10MB
log4j.appender.JSON.MaxBackupIndex=1

# Use this appender when running data-info in the foreground.
log4j.appender.B=org.apache.log4j.ConsoleAppender
log4j.appender.B.layout=org.apache.log4j.PatternLayout
log4j.appender.B.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n

# Use this appender when running data-info in the background.
log4j.appender.A=org.apache.log4j.RollingFileAppender
log4j.appender.A.File=/var/log/data-info/data-info.log
log4j.appender.A.layout=org.apache.log4j.PatternLayout
log4j.appender.A.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.A.MaxFileSize=10MB
log4j.appender.A.MaxBackupIndex=1
```

See the [log4j documentation](http://logging.apache.org/log4j/1.2/manual.html)
for additional logging configuration instructions.
