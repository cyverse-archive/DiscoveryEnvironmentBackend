# Table of Contents

* [Miscellaneous Endpoints](#miscellaneous-endpoints)
    * [Verifying that metadactyl-clj is Running](#verifying-that-metadactyl-clj-is-running)

# Miscellaneous Endpoints

## Verifying that metadactyl-clj is Running

*Unsecured Endpoint:* GET /

The root path in metadactyl-clj can be used to verify that metadactyl-clj is
actually running and is responding. Currently, the response to this URL contains
only a welcome message. Here's an example:

```
$ curl -s http://by-tor:8888/
Welcome to Metadactyl!
```
