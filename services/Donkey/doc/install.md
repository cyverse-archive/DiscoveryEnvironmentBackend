# Installing and Configuring Donkey

Donkey is packaged as an RPM and published in iPlant's YUM repositories.  It
can be installed using `yum install donkey` and upgraded using `yum upgrade
donkey`.

## Primary Configuration

Donkey reads in its configuration from a file. By default, it will look for
the file at /etc/iplant/de/donkey.properties, but you can override the
path by passing Donkey the --config setting at start up.

Here's an example configuration file:

```properties
# Connection details.
donkey.app.listen-port = 65002

# Environment information.
donkey.app.environment-name = example

# iPlant Email service connection settings.
donkey.email.base-url          = http://localhost:65003
donkey.email.tool-request-dest = somebody@iplantcollaborative.org
donkey.email.tool-request-src  = nobody@iplantcollaborative.org

# Metadactyl connection settings
donkey.metadactyl.base-url             = http://localhost:65007/secured
donkey.metadactyl.unprotected-base-url = http://localhost:65007

# Notification agent connection settings.
donkey.notificationagent.base-url = http://localhost:65011

# CAS Settings
donkey.cas.cas-server  = https://cas-server.iplantcollaborative.org/cas/
donkey.cas.server-name = http://localhost:65002

# The domain name to append to the user id to get the fully qualified user id.
donkey.uid.domain = iplantcollaborative.org

# User information lookup settings.
donkey.userinfo.base-url             = https://localhost/api/v1
donkey.userinfo.client-key           = some-client-key
donkey.userinfo.password             = some-client-password
donkey.userinfo.default-search-limit = 50

# Nibblonian connection settings
donkey.nibblonian.base-url = http://localhost:65010/

# JEX connection settings
donkey.jex.base-url = http://localhost:65006/

# Scruffian connection settings
donkey.scruffian.base-url = http://localhost:65013/

# Tree viewer settings
donkey.tree-viewer.base-url              = http://localhost/parseTree

# Elastic Search settings
donkey.infosquito.es-url = http://services-2.iplantcollaborative.org:31338
```

Generally, the service connection settings will have to be updated for each
deployment.

## Zookeeper Connection Information

One piece of information that can't be stored in Zookeeper is the information
required to connect to Zookeeper.  For Donkey and most other iPlant services,
this information is stored in a single file:
`/etc/iplant-services/zkhosts.properties`.  This file is automatically
installed when the iplant-service-configs RPM is installed.  You may have to
modify this file so that it points to the correct hosts.

## Logging Configuration

The logging settings are stored in `/etc/donkey/log4j.properties`.  The file
looks like this by default:

```properties
log4j.rootLogger=WARN, A

# Uncomment these lines to enable debugging in Donkey itself.
# log4j.category.donkey=DEBUG, A
# log4j.additivity.donkey=false

# Uncomment these lines to enable debugging in iPlant Clojure Commons.
# log4j.category.clojure-commons=DEBUG, A
# log4j.additivity.clojure-commons=false

# Either comment these lines out or change the appender to B when running
# Donkey in the foreground.
log4j.logger.JsonLogger=debug, JSON
log4j.additivity.JsonLogger=false

# Use this appender for logging JSON when running Donkey in the background.
log4j.appender.JSON=org.apache.log4j.RollingFileAppender
log4j.appender.JSON.File=/var/log/donkey/json.log
log4j.appender.JSON.layout=org.apache.log4j.PatternLayout
log4j.appender.JSON.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.JSON.MaxFileSize=10MB
log4j.appender.JSON.MaxBackupIndex=1

# Use this appender when running Donkey in the foreground.
log4j.appender.B=org.apache.log4j.ConsoleAppender
log4j.appender.B.layout=org.apache.log4j.PatternLayout
log4j.appender.B.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n

# Use this appender when running Donkey in the background.
log4j.appender.A=org.apache.log4j.RollingFileAppender
log4j.appender.A.File=/var/log/donkey/donkey.log
log4j.appender.A.layout=org.apache.log4j.PatternLayout
log4j.appender.A.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.A.MaxFileSize=10MB
log4j.appender.A.MaxBackupIndex=1
```

See the [log4j documentation](http://logging.apache.org/log4j/1.2/manual.html)
for additional logging configuration instructions.
