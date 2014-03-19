# Table of Contents

* [Miscellaneous Endpoints](#miscellaneous-endpoints)
    * [Verifying that metadactyl-clj is Running](#verifying-that-metadactyl-clj-is-running)
    * [Initializing a User's Workspace](#initializing-a-users-workspace)

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

## Initializing a User's Workspace

*Secured Endpoint:* GET /secured/bootstrap

The DE calls this service as soon as the user logs in. This service always
records the fact that the user logged in, and it also initializes the user's
workspace if the user has never logged in before. If the service succeeds then
the response body is in the following format:

```json
{
    "action": "bootstrap",
    "loginTime": login-milliseconds,
    "newWorkspace": new-workspace-flag,
    "status": "success",
    "workspaceId": workspace-id,
    "usermame": username,
    "email": email-address,
    "firstName": first-name,
    "lastName": last-name
}
```

This service required three query-string parameters:

* user - the short version of the username
* email - the user's email address
* ip-address - the source IP address of the login request

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/bootstrap?user=snow-dog&email=sd@example.org&first-name=Snow&last-name=Dog&ip-address=127.0.0.1" | python -mjson.tool
{
    "action": "bootstrap",
    "loginTime": "1374180749466",
    "newWorkspace": false,
    "status": "success",
    "workspaceId": "4",
    "username": "snow-dog",
    "email": "sd@example.org",
    "firstName": "Snow",
    "lastName": "Dog"
}
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
