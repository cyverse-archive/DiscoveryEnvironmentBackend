# anon-files

A service that serves up files that have been shared with the anonymous user in iRODS.

## Build

    lein uberjar

## Configuration

anon-files accepts configuration files in the EDN format. See [conf/anon-files.edn](conf/anon-files.edn) for an example. If a config file is not given on the command-line, anon-files will look for one at __/etc/anon-files/anon-files.edn__.

anon-files is able to reconfigure itself at run-time with changes contained in the config. It is recommended that you use this feature sparingly, because an invalid config may cause issues that require a restart anyway. When in doubt, restart.

To force a reconfiguration at run-time, touch the config file. Avoid editing the file in-place. Instead, copy the existing config, make the changes in the copy, and move the new config into place.

Forcing reconfiguration does not work for the log4j settings (used by a dependency) or for the port setting.

Many of the command-line options accepted by anon-files have a corresponding config file setting. The command-line setting __always__ overrides the config file setting, even if you reload the config file as described above.

Here are the command-line options:

    A service that serves up files shared with the iRODS anonymous user.

    Usage: anon-files [options]

    Options:
      -p, --port PORT                                       Port number
      -c, --config PATH      /etc/iplant/de/anon-files.edn  Path to the config file
      -v, --version                                         Print out the version number.
      -d, --disable-log4j                                   Disables log4j logging. Timbre logging still enabled.
      -f, --log-file PATH                                   Path to the log file.
      -s, --log-size SIZE                                   Max Size of the logs in MB.
      -b, --log-backlog MAX                                 Max number of rotated logs to retain.
      -l, --log-level LEVEL                                 One of: trace debug info warn error fatal report
      -h, --help

The --disable-log4j option is intended for use during development and is not available in the config file. The port option cannot be changed at run-time, regardless of whether it was specified on the command-line or in the config file.

Here is a config file with all of the available settings:
```clojure
{:port 31302
 :irods-host "HOSTNAME"
 :irods-port "1247"
 :irods-zone "iplant"
 :irods-home "/iplant/home"

 ; This should not be the anonymous user. It needs to be a user that has
 ; read access to the files in iRODS.
 :irods-user "USERNAME"
 :irods-password "PASSWORD"

 ; This *should* be the anonymous user. I can't think of a reason why it
 ; wouldn't be set to "anonymous", but I made it configurable just in case.
 :anon-user "anonymous"

 :log-file "/var/log/anon-files/anon-files.log"

 ;one of :trace :debug :info :warn :error :fatal :report
 :log-level :info

 ;Max number of rotated logs to retain
 :log-backlog 5

 ;Max size of an individual log
 :log-size 1024}
```

## Running it

For local development:

    lein run -- --config /path-to-config --disable-log4j

From an uberjar:

    java -jar anon-files-3.1.0-standalone.jar --config /path/to/config

For the uberjar, make sure the log4j settings are in the classpath. A log4j config is located in (conf/log4j.properties)[conf/log4j.properties].


## License

See [LICENSE](LICENSE)
