jex-events
==========

jex-events receives events sent over an AMQP exchange and logs them into the jex
database. Additionally, it provides a small interface that allows callers to
add new jobs to the JEX database.


# Configuration

jex-events has a JSON encoded configuration that looks like the following:

```json
{
  "AMQPURI" : "amqp://<user>:<password>@<hostname>:<port>/",
  "ExchangeName" : "exchange",
  "ExchangeType" : "fanout",
  "RoutingKey" : "key",
  "ExchangeDurable" : true,
  "ExchangeAutodelete" : false,
  "ExchangeInternal" : false,
  "ExchangeNoWait" : false,
  "QueueName" : "queue",
  "QueueBindingKey" : "binding_key",
  "QueueDurable" : true,
  "QueueAutodelete" : false,
  "QueueInternal" : false,
  "QueueNoWait" : false,
  "ConsumerTag" : "tag",
  "DBURI" : "postgres://<username>:<password>@<hostname>:<port>/<dbname>?sslmode=disable",
  "HTTPListenPort" : ":8080"
}
```

You can pass the path to the configuration file with the --config option.

# Running it

jex-events logs to stdout and runs in the foreground. An external tool like
supervisord is suggested to daemonize the service. Here's a sample of how to
manually start it up:

```
jex-events --config /path/to/config.json
```

# Building it

jex-events is written in Go [Go](http://golang.org), so you'll need the Go
toolchain installed. Dependencies can be retrieved with [godeps](https://github.com/tools/godep).
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

# Inserting a job over HTTP/JSON

The incoming JSON must have the following format:
```json
{
  "Submitter"   : "<string>",
  "AppID"       : "<uuid>",
  "CommandLine" : "<string>",
  "CondorID"    : "<string>"
}
```
Those are the required fields. The following fields are also accepted:
```json
{
  "BatchID"          : "<uuid>",
  "DateSubmitted"    : "<timestamp>",
  "DateStarted"      : "<timestamp>",
  "DateCompleted"    : "<timestamp>",
  "EnvVariables"     : "<string>",
  "ExitCode"         : <int>,
  "FailureCount"     : <int>,
  "FailureThreshold" : <int>
}
```

Any fields marked as <timestamp> must be a string formatted according to
RFC3339. Here's an example from the Go programming language docs:
  2006-01-02T15:04:05Z07:00
The timezone *is* stored with dates, so you'll probably want to convert to
UTC before sending the timestamps in the JSON.

For now, I'd recommend only sending the required JSON. The rest of the fields are
either unused (for now) or are filled in by Condor events that arrive through
the AMQP interface.

Errors will return either a 400 or 500 series HTTP status and an error message.

Successful calls will return with a 200 series HTTP status.
