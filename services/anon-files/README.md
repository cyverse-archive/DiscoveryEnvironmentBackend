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

## Running it

For local development:

    lein run -- --config /path-to-config --disable-log4j

From an uberjar:

    java -jar anon-files-3.1.0-standalone.jar --config /path/to/config

For the uberjar, make sure the log4j settings are in the classpath. A log4j config is located in (conf/log4j.properties)[conf/log4j.properties].


## License

See [LICENSE](LICENSE)
