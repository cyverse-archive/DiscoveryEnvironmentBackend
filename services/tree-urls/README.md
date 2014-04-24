# tree-urls

A service for managing saved searches in the DE.

## Build

    lein uberjar

## Configuration

tree-urls accepts configuration files in the EDN format. See [conf/tree-urls.edn](conf/tree-urls.edn) for an example. If a config file is not given on the command-line, tree-urls will look for one at __/etc/tree-urls/tree-urls.edn__.

tree-urls is able to reconfigure itself at run-time with changes contained in the config. It is recommended that you use this feature sparingly, because an invalid config may cause issues that require a restart anyway. When in doubt, restart.

To force a reconfiguration at run-time, touch the config file. Avoid editing the file in-place. Instead, copy the existing config, make the changes in the copy, and move the new config into place.

Forcing reconfiguration does not work for the port or database settings since reconfiguring those at run-time would result in failed requests.

Many of the command-line options accepted by tree-urls have a corresponding config file setting. The command-line setting __always__ overrides the config file setting, even if you reload the config file as described above.

Here are the command-line options:

    DE API for managing tree urls.

    Usage: tree-urls [options]

    Options:
      -p, --port PORT                                          Port number
      -c, --config PATH      /etc/iplant/de/tree-urls.edn  Path to the config file
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
 :port 31307
 :log-file "/var/log/tree-urls/tree-urls.log"

 ;one of :trace :debug :info :warn :error :fatal :report
 :log-level :info

 ;Max number of rotated logs to retain
 :log-backlog 5

 ;Max size of an individual log
 :log-size 1024}
```

tree-urls listens on port 31304 by default.

## Running it

For local development:

    lein run -- --config /path/to/config

From an uberjar:

    java -jar tree-urls-3.1.0-standalone.jar --config /path/to/config

If you installed tree-urls through an RPM, make sure the config file is at /etc/iplant/de/ (make the directory if necessary). Then run the following:

    sudo /sbin/service tree-urls start

# LICENSE

See [LICENSE](LICENSE)

