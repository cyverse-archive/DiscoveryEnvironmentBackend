# saved-searches

A service for managing saved searches in the DE.

## Build

    lein uberjar

## Configuration

saved-searches accepts configuration files in the EDN format. See [conf/saved-searches.edn](conf/saved-searches.edn) for an example. If a config file is not given on the command-line, saved-searches will look for one at __/etc/saved-searches/saved-searches.edn__.

saved-searches is able to reconfigure itself at run-time with changes contained in the config. It is recommended that you use this feature sparingly, because an invalid config may cause issues that require a restart anyway. When in doubt, restart.

To force a reconfiguration at run-time, touch the config file. Avoid editing the file in-place. Instead, copy the existing config, make the changes in the copy, and move the new config into place.

Forcing reconfiguration does not work for the port or database settings since reconfiguring those at run-time would result in failed requests.

Many of the command-line options accepted by saved-searches have a corresponding config file setting. The command-line setting __always__ overrides the config file setting, even if you reload the config file as described above.

Here are the command-line options:

    DE API for managing saved searches.

    Usage: saved-searches [options]

    Options:
      -p, --port PORT                                          Port number
      -c, --config PATH      /etc/iplant/de/saved-searches.edn  Path to the config file
      -v, --version                                            Print out the version number.
      -f, --log-file PATH                                      Path to the log file.
      -s, --log-size SIZE                                      Max Size of the logs in MB.
      -b, --log-backlog MAX                                    Max number of rotated logs to retain.
      -l, --log-level LEVEL                                    One of: trace debug info warn error fatal report
      -h, --help

Here is a config file with all of the available settings:
```clojure
{:db-name "test"
 :db-user "test"
 :db-password "testpassword"
 :db-host "databasehost"
 :db-port "5432"
 :port 31306
 :log-file "/var/log/saved-searches/saved-searches.log"

 ;one of :trace :debug :info :warn :error :fatal :report
 :log-level :info

 ;Max number of rotated logs to retain
 :log-backlog 5

 ;Max size of an individual log
 :log-size 1024}
```

saved-searches listens on port 31304 by default.

## Running it

For local development:

    lein run -- --config /path/to/config

From an uberjar:

    java -jar saved-searches-3.1.0-standalone.jar --config /path/to/config

If you installed saved-searches through an RPM, make sure the config file is at /etc/iplant/de/ (make the directory if necessary). Then run the following:

    sudo /sbin/service saved-searches start


## API

### Getting saved searches for a user

    GET /<username>

Returns the JSON containing the saved searches for the user. Sample curl command:

    curl http://localhost:31306/ipctest@iplantcollaborative.org


### Updating/creating saved searches for a user

    POST /<username>

Or,

    PUT /<username>

The body of the request should be JSON containing all of the saved searches for the user. The format of the body is not enforced. Any JSON should work.

The Content-Type for the request must be "application/json".

Sample Curl command:

    curl -H "Content-Type: application/json" -d '{"foo" : "bar"}' http://localhost:31306/ipctest@iplantcollaborative.org


### Deleting saved searches for a user

    DELETE /<username>

Sample curl command:

    curl -X DELETE http://localhost:31306/ipctest@iplantcollaborative.org

This destroys ALL saved searches for the user. To remove a single saved search, get the saved searches, update the JSON to remove the unwanted saved search, and POST the new JSON.

### Error reporting

If the user doesn't exist, you will receive a 404 status and a message similar to the following:

    {"user" : "ipctest"}

JSON parsing and error reporting is handled by [ring-json](https://github.com/ring-clojure/ring-json). If you post malformed JSON, you will receive a 400 status and a message like this will be returned to the client:

    Malformed JSON in request body.

If you attempt to POST a request with the wrong content-type (as in, not "application/json"), you will receive a response with a status of 415 and the body of the response will be something like this:

    {"content-type":"application/js"}

If a server-side error occurs while handling a request, the request will fail with a 500 status and a stacktrace as plain text. The stack trace will contain ANSI font coloring.


# LICENSE

See [LICENSE](LICENSE)

