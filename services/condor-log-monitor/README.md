condor-log-monitor
==================

The condor-log-monitor is a daemon that monitors a HTCondor EVENT_LOG for changes
and pushes job updates out to an AMQP broker.

# What does it do?

condor-log-monitor (aka 'clm') periodically checks the last modified date of the
configured HTCondor EVENT_LOG (configured as EventLog, as seen below) and will
parse it for updates when the date changes. Each parsed out event is sent to the
configured AMQP exchange, where interested services can receive the events.

clm also stores a 'tombstone' file at __/tmp/condor-log-monitor.tombstone__.
This file contains the record of the last parsed inode, the position that
parsing ended on in that file, the date that the tombstone was made, and the
last modified date of the file pointed to by the inode number.

The tombstone is used to resume parsing if condor-log-monitor goes down for a
while. It allows the condor-log-monitor to detect if the EVENT_LOG as rolled
over while it was down and attempt to resume parsing from the rolled over logs.
HTCondor needs to be configured to roll over the logs with a numerical suffix on
the rolled logs (go to http://research.cs.wisc.edu/htcondor/manual/v7.8/3_3Configuration.html#SECTION004310000000000000000 and search for EVENT_LOG).


# Configuration

condor-log-monitor is configured with a JSON configuration file. The JSON file
should look like this:

```json
{
  "AMQPHost" : "hostname:5672",
  "AMQPUserPass" : "user:password",
  "ExchangeName" : "exchange",
  "ExchangeType" : "direct",
  "RoutingKey" : "condor.events",
  "Durable" : true,
  "Autodelete" : false,
  "Internal" : false,
  "NoWait" : false,
  "EventLog" : "/path/to/event_log"
}
```

# Running it

condor-log-monitor logs to stdout and runs in the foreground by default. Here's
a typical command-line to start it up:

```
$ ./condor-log-monitor --config /path/to/config.json
```

# Building it

condor-log-monitor is written in [Go](http://golang.org), so you'll need the Go
toolchain installed. Also, dependencies can be retrieved with [godeps](https://github.com/tools/godep).
Once godeps is installed, you can do the following:

```bash
godep restore
go build
```

If you're doing development on OS X but running on Linux, you'll want to set up
cross-compilation for Go. After that's done, the builds will look like this:

```bash
godep restore
GOOS=linux go build
```

To run the unit tests:

```bash
go test
```
