# job-preserver

A Clojure utility used to migrate remaining jobs from the OSM MongoDB database to the DE Postgres
database.

## Usage

```
java -jar job-preserver.jar --pg-host host --pg-port port --pg-user user --mongo-host host \
    --mongo-port port
```

There is no command-line option for the PostgreSQL password. Instead, the password is obtained from
the user's `.pgpass` file.
