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

## Recording when the User Logs Out

*Secured Endpoint:* GET /secured/logout

The DE calls this service when the user explicitly logs out. This service simply
records the time that the user logged out in the login record created by the
`/secured/bootstrap` service. This service requires four query-string
parameters:

* user - the short version of the username
* email - the user's email address
* ip-address - the source IP address of the logout request
* login-time - the login timestamp that was returned by the bootstrap service

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/bootstrap?user=snow-dog&email=sd@example.org&ip-address=127.0.0.1&login-time=1374180749466" | python -mjson.tool
{
    "action": "logout",
    "status": "success"
}
```
