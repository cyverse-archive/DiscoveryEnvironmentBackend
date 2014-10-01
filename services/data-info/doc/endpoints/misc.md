# Table of Contents

* [Miscellaneous data-info Endpoints](#miscellaneous-data-info-endpoints)
    * [Verifying that data-info is Running](#verifying-that-data-info-is-running)
    * [Initializing a User's Workspace](#initializing-a-users-workspace)
    * [Saving User Session Data](#saving-user-session-data)
    * [Retrieving User Session Data](#retrieving-user-session-data)
    * [Removing User Session Data](#removing-user-seession-data)
    * [Saving User Preferences](#saving-user-preferences)
    * [Retrieving User Preferences](#retrieving-user-preferences)
    * [Removing User Preferences](#removing-user-preferences)
    * [Saving User Search History](#saving-user-search-history)
    * [Retrieving User Search History](#retrieving-user-search-history)
    * [Deleting User Search History](#deleting-user-search-history)
    * [Determining a User's Default Output Directory](#determining-a-users-default-output-directory)
    * [Resetting a user's default output directory.](#resetting-a-users-default-output-directory.)
    * [Obtaining Identifiers](#obtaining-identifiers)
    * [Submitting User Feedback](#submitting-user-feedback)
    * [Getting a user's saved searches](#getting-saved-searches)
    * [Setting a user's saved searches](#setting-saved-searches)
    * [Deleting a user's saved searches](#deleting-saved-searches)

# Miscellaneous data-info Endpoints

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](data-info-v-metadactyl.md) for more
information.

## Verifying that data-info is Running

Unsecured Endpoint: GET /

The root path in data-info can be used to verify that data-info is actually running
and is responding. Currently, the response to this URL contains only a welcome
message. Here's an example:

```
$ curl -s http://by-tor:8888/
Welcome to data-info!  I've mastered the stairs!
```

## Initializing a User's Workspace and Preferences

Secured Endpoint: GET /secured/bootstrap

The DE calls this service as soon as the user logs in to initialize the user's workspace if the user
has never logged in before, and returns user information, including the user's preferences, the
user's home path, the user's trash path, and the base trash. This service always records the fact
that the user logged in.

Note that the required `ip-address` query parameter cannot be obtained automatically in most cases.
Because of this, the `ip-address` parameter must be passed to this service in addition to the
`proxyToken` parameter. Here's an example:

```json
$ curl "http://by-tor:8888/secured/bootstrap?proxyToken=$(cas-ticket)&ip-address=127.0.0.1" | python -mjson.tool
{
    "action": "bootstrap",
    "loginTime": "1374190755304",
    "newWorkspace": false,
    "status": "success",
    "workspaceId": "4",
    "username": "snow-dog",
    "email": "sd@example.org",
    "firstName": "Snow",
    "lastName": "Dog",
    "userHomePath": "/iplant/home/snow-dog",
    "userTrashPath": "/iplant/trash/snow-dog",
    "baseTrashPath": "/iplant/trash",
    "preferences": {
        "systemDefaultOutputDir": {
            "id": "/iplant/home/snow-dog/analyses",
            "path": "/iplant/home/snow-dog/analyses"
        },
        "defaultOutputFolder": {
            "id": "/iplant/home/snow-dog/analyses",
            "path": "/iplant/home/snow-dog/analyses"
        }
    }
}
```

## Recording when a User Logs Out

Secured Endpoint: GET /secured/logout

The DE calls this service when the user explicitly logs out. This service simply records the time
that the user logged out in the login record created by the `/secured/bootstrap` service.
Note that this service requires these query-string parameters, which cannot be obtained
automatically in most cases, in addition to the `proxyToken` parameter:

* ip-address - the source IP address of the logout request
* login-time - the login timestamp that was returned by the bootstrap service

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/logout?proxyToken=$(cas-ticket)&ip-address=127.0.0.1&login-time=1374190755304" | python -mjson.tool
{
    "action": "logout",
    "status": "success"
}
```

## Saving User Session Data

Secured Endpoint: POST /secured/sessions

This service can be used to save arbitrary JSON user session information. The
post body is stored as-is and can be retrieved by sending an HTTP GET request
to the same URL.

Here's an example:

```
$ curl -sd '{"foo":"bar"}' "http://by-tor:8888/secured/sessions?proxyToken=$(cas-ticket)"
```

## Retrieving User Session Data

Secured Endpoint: GET /secured/sessions

This service can be used to retrieve user session information that was
previously saved by sending a POST request to the same service.

Here's an example:

```
$ curl "http://by-tor:8888/secured/sessions?proxyToken=$(cas-ticket)"
{"foo":"bar"}
```

## Removing User Session Data

Secured Endpoint: DELETE /secured/sessions

This service can be used to remove saved user session information. This is
helpful in cases where the user's session is in an unusable state and saving the
session information keeps all of the user's future sessions in an unusable
state.

Here's an example:

```
$ curl -XDELETE "http://by-tor:8888/secured/sessions?proxyToken=$(cas-ticket)"
```

Check the HTTP status of the response to tell if it succeeded. It should return
a status in the 200 range.

An attempt to remove session data that doesn't already exist will be silently
ignored and return a 200 range HTTP status code.

## Saving User Preferences

Secured Endpoint: POST /secured/preferences

This service can be used to save arbitrary user preferences. The body must contain
all of the preferences for the user; any key-value pairs that are missing will be
removed from the preferences. Please note that the "defaultOutputDir" and the
"systemDefaultOutputDir" will always be present, even if not included in the
JSON passed in.

Example:

```
$ curl -sd '{"appsKBShortcut":"A","rememberLastPath":true,"closeKBShortcut":"Q","defaultOutputFolder":{"id":"/iplant/home/wregglej/analyses","path":"/iplant/home/wregglej/analyses"},"dataKBShortcut":"D","systemDefaultOutputDir":{"id":"/iplant/home/wregglej/analyses","path":"/iplant/home/wregglej/analyses"},"saveSession":true,"enableEmailNotification":true,"lastPathId":"/iplant/home/wregglej","notificationKBShortcut":"N","defaultFileSelectorPath":"/iplant/home/wregglej","analysisKBShortcut":"Y"}' "http://by-tor:8888/secured/preferences?proxyToken=$(cas-ticket)" | squiggles
{
    "preferences": {
        "analysisKBShortcut": "Y",
        "appsKBShortcut": "A",
        "closeKBShortcut": "Q",
        "dataKBShortcut": "D",
        "defaultFileSelectorPath": "/iplant/home/wregglej",
        "defaultOutputFolder": {
            "id": "/iplant/home/wregglej/analyses",
            "path": "/iplant/home/wregglej/analyses"
        },
        "enableEmailNotification": true,
        "lastPathId": "/iplant/home/wregglej",
        "notificationKBShortcut": "N",
        "rememberLastPath": true,
        "saveSession": true,
        "systemDefaultOutputDir": {
            "id": "/iplant/home/wregglej/analyses",
            "path": "/iplant/home/wregglej/analyses"
        }
    },
    "success": true
}
```

## Retrieving User Preferences

Secured Endpoint: GET /secured/preferences

This service can be used to retrieve a user's preferences.

Example:

```
$ curl -s "http://by-tor:8888/secured/preferences?proxyToken=$(cas-ticket)" | squiggles
{
    "analysisKBShortcut": "Y",
    "appsKBShortcut": "A",
    "closeKBShortcut": "Q",
    "dataKBShortcut": "D",
    "defaultFileSelectorPath": "/iplant/home/test",
    "defaultOutputFolder": {
        "id": "/iplant/home/test/analyses",
        "path": "/iplant/home/test/analyses"
    },
    "enableEmailNotification": true,
    "lastPathId": "/iplant/home/test",
    "notificationKBShortcut": "N",
    "rememberLastPath": true,
    "saveSession": true,
    "systemDefaultOutputDir": {
        "id": "/iplant/home/test/analyses",
        "path": "/iplant/home/test/analyses"
    }
}
```

## Removing User Preferences

Secured Endpoint: DELETE /secured/preferences

This service can be used to remove a user's preferences.

Please note that the "defaultOutputDir" and the "systemDefaultOutputDir" will
still be present in the preferences after a deletion.

Example:

```
$ curl -X DELETE "http://by-tor:8888/secured/preferences?proxyToken=$(cas-ticket)"
```

Check the HTTP status code of the response to determine success. It should be in the 200 range.

An attempt to remove preference data that doesn't already exist will be silently
ignored.

## Saving User Search History

Secured Endpoint: POST /secured/search-history

This service can be used to save arbitrary user search history information. The
POST body is stored without modification and be retrieved by sending a GET
request to the same URL.

Example:

```
$ curl -sd data "http://by-tor:8888/secured/search-history?proxyToken=$(cas-ticket)"
data
```

## Retrieving User Search History

Secured Endpoint: GET /secured/search-history

This service can be used to retrieve a user's search history.

Example:

```
$ curl -s "http://by-tor:8888/secured/search-history?proxyToken=$(cas-ticket)"
data
```

## Deleting User Search History

This service can be used to delete a user's search history.

Example:

```
$ curl -XDELETE -s "http://by-tor:8888/secured/search-history?proxyToken=$(cas-ticket)"
{
    "success" : true
}
```

## Determining a User's Default Output Directory

Secured Endpoint: GET /secured/default-output-dir

This endoint determines the default output directory in iRODS for the currently
authenticated user. Aside from the `proxyToken` parameter, this endpoint
requires no query-string parameters. The default default output directory name
is passed to data-info in the `data-info.job-exec.default-output-folder` configuration
parameter.

This service works in conjunction with user preferences. If a default output
directory has been selected already (either by the user or automatically) then
this service will attempt to use that directory. If that directory exists
already then this service will just return the full path to the directory. If
the path exists and refers to a regular file then the service will fail with an
error code of `REGULAR-FILE-SELECTED-AS-OUTPUT-FOLDER`. Otherwise, this service
will create the directory and return the path.

If the default output directory has not been selected yet then this service will
automatically generate the path to the directory based on the name that was
given to data-info in the `data-info.job-exec.default-output-folder` configuration
setting. The value of this configuration setting is treated as being relative to
the user's home directory in iRODS. If the path exists and is a directory then
the path is saved in the user's preferences and returned. If the path does not
exist then the directory is created and the path is saved in the user's
preferences and returned. If the path exists and is a regular file then the
service will generate a unique path (by repeatedly trying the same name with a
hyphen and an integer appended to it) and update the preferences and return the
path when a unique path is found.

Upon success, the JSON object returned in the response body contains a flag
indicating that the service call was successfull along with the full path to the
default output directory. Upon failure, the response body contains a flag
indicating that the service call was not successful along with some information
about why the service call failed.

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/default-output-dir?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "path": "/iplant/home/ipctest/analyses",
    "success": true
}
```

## Resetting a user's default output directory.

Secured Endpoint: POST /secured/default-output-dir

This endpoint resets a user's default output directory to its default value even
if the user has already chosen a different default output directory.  Since this
is a POST request, this request requires a message body. The message body in
this case is a JSON object containing the path relative to the user's home
directory in the `path` attribute. Here are some examples:

```
$ curl -sd '
{
    "path":"foon"
}' "http://by-tor:8888/secured/default-output-dir?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "path": "/iplant/home/ipctest/foon",
    "success": true
}
```

```
$ curl -sd '
{
    "inv":"foon"
}' "http://by-tor:8888/secured/default-output-dir?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "arg": "path",
    "code": "MISSING-REQUIRED-ARGUMENT",
    "success": false
}
```

## Obtaining Identifiers

Unsecured Endpoint: GET /uuid

In some cases, it's difficult for the UI client code to generate UUIDs for
objects that require them. This service returns a single UUID in the response
body. The UUID is returned as a plain text string.

## Submitting User Feedback

Secured Endpoint: PUT /secured/feedback

This endpoint submits feedback from the user to a configurable iPlant email
address. The destination email address is stored in the configuration settting,
`data-info.email.feedback-dest`. The request body is a simple JSON object with the
question text in the keys and the answer or answers in the values. The answers
can either be strings or lists of strings:

```json
{
    "question 1": "question 1 answer 1",
    "question 2": [
        "question 2 answer 1",
        "question 2 answer 2"
    ]
}
```

Here's an example:

```
$ curl -XPUT -s "http://by-tor:8888/secured/feedback?proxyToken=$(cas-ticket)" -d '
{
    "What is the circumference of the Earth?": "Roughly 25000 miles.",
    "What are your favorite programming languages?": [ "Clojure", "Scala", "Perl" ]
}
' | python -mjson.tool
{
    "success": true
}
```

## Saved Searches

The saved-search endpoint proxies requests to the saved-searches service. This endpoint
is used to store, retrieve, and delete a user's saved searches.


### Getting saved searches

Secured Endpoint: GET /secured/saved-searches

Curl example:

     curl http://localhost:31325/secured/saved-searches?proxyToken=not-real

The response body will be JSON. The service endpoint doesn't have a particular JSON
structure it looks for, it simply stores whatever JSON is passed to it.

Possible error codes: ERR_BAD_REQUEST, ERR_NOT_A_USER, ERR_UNCHECKED_EXCEPTION

### Setting saved searches

Secured Endpoint: POST /secured/saved-searches

Curl example:

     curl -d '{"foo":"bar"}' http://localhost:31325/secured/saved-searches?proxyToken=not-real

Response body:

```json
{
        "success" : true,
        "saved_searches" : {"foo":"bar"}
}
```

Possible error codes: ERR_BAD_REQUEST, ERR_NOT_A_USER, ERR_UNCHECKED_EXCEPTION

If you pass up invalid JSON, you'll get an error like the following:

   {"success":false,"reason":"Cannot JSON encode object of class: class org.eclipse.jetty.server.HttpInput: org.eclipse.jetty.server.HttpInput@1cbeb264"}

### Deleting saved searches

Secured endpoint : DELETE /secured/saved-searches

Curl example:

     curl -X DELETE http://localhost:31325/secured/saved-searches?proxyToken=not-real

You should get a response body back like the following:

```json
{"success":true}
```
