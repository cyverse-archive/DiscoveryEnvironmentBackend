# Table of Contents

* [Miscellaneous Donkey Endpoints](#miscellaneous-donkey-endpoints)
    * [Verifying that Donkey is Running](#verifying-that-donkey-is-running)
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

# Miscellaneous Donkey Endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Verifying that Donkey is Running

Unsecured Endpoint: GET /

The root path in Donkey can be used to verify that Donkey is actually running
and is responding. Currently, the response to this URL contains only a welcome
message. Here's an example:

```
$ curl -s http://by-tor:8888/
Welcome to Donkey!  I've mastered the stairs!
```

## Initializing a User's Workspace

Secured Endpoint: GET /secured/bootstrap

This endpoint gets user information from the metadactyl endpoint using the same
path, and adds the user's home path, the user's trash path, and the base trash
path to the response.
Please see the [metadactyl documentation](https://github.com/iPlantCollaborativeOpenSource/metadactyl-clj/blob/master/doc/endpoints/misc.md#initializing-a-users-workspace) for more information.

Note that the `ip-address` query parameter that has to be passed to the
metadactyl service cannot be obtained automatically in most cases. Because of
this, the `ip-address` parameter must be passed to this service in addition to
the `proxyToken` parameter. Here's an example:

```
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
    "baseTrashPath": "/iplant/trash"
}
```

## Recording when a User Logs Out

Secured Endpoint: GET /secured/logout

Delegates to metadactyl: GET /secured/logout

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

Note that the `ip-address` and `login-time` query parameters that have to be
passed to the metadactyl service cannot be obtained automatically in most cases.
Because of this, these parameters must be passed to this service in addition to
the `proxyToken` parameter. Here's an example:

```
$ curl -s "http://by-tor:8888/secured/logout?proxyToken=$(cas-ticket)&ip-address=127.0.0.1&login-time=1374190755304" | python -mjson.tool
{
    "action": "logout",
    "status": "success"
}
```

## Saving User Session Data

Secured Endpoint: POST /secured/sessions

This service can be used to save arbitrary user session information. The post
body is stored as-is and can be retrieved by sending an HTTP GET request to the
same URL.

Here's an example:

```
$ curl -sd data "http://by-tor:8888/secured/sessions?proxyToken=$(cas-ticket)"
```

## Retrieving User Session Data

Secured Endpoint: GET /secured/sessions

This service can be used to retrieve user session information that was
previously saved by sending a POST request to the same service.

Here's an example:

```
$ curl "http://by-tor:8888/secured/sessions?proxyToken=$(cas-ticket)"
data
```

## Removing User Session Data

Secured Endpoint: DELETE /secured/sessions

This service can be used to remove saved user session information. This is
helpful in cases where the user's session is in an unusable state and saving the
session information keeps all of the user's future sessions in an unusable
state.

Here's an example:

```
$ curl -XDELETE "http://by-tor:8888/secured/sessions?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true
}
```

An attempt to remove session data that doesn't already exist will be silently
ignored.

## Saving User Preferences

Secured Endpoint: POST /secured/preferences

This service can be used to save arbitrary user preferences. The body must contain
all of the preferences for the user; any key-value pairs that are missing will be
removed from the preferences. Please note that the "defaultOutputDir" and the 
"systemDefaultOutputDir" will always be present, even if not included in the
JSON passed in.

Example:

```
$ curl -sd data "http://by-tor:8888/secured/preferences?proxyToken=$(cas-ticket)"
data
```

## Retrieving User Preferences

Secured Endpoint: GET /secured/preferences

This service can be used to retrieve a user's preferences.

Example:

```
$ curl -s "http://by-tor:8888/secured/preferences?proxyToken=$(cas-ticket)"
data
```

## Removing User Preferences

Secured Endpoint: DELETE /secured/preferences

This service can be used to remove a user's preferences. 

Please note that the "defaultOutputDir" and the "systemDefaultOutputDir" will 
still be present in the preferences after a deletion.

Example:

```
$ curl -X DELETE "http://by-tor:8888/secured/preferences?proxyToken=$(cas-ticket)"
{
    "success" : true
}
```

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
is passed to Donkey in the `donkey.job-exec.default-output-folder` configuration
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
given to Donkey in the `donkey.job-exec.default-output-folder` configuration
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
`donkey.email.feedback-dest`. The request body is a simple JSON object with the
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


## Adding data to a user's bucket

Secured Endpoint: POST /secured/buckets/:username/:bucket/:key

The body should be anything that can be returned as a string, though there's no
checking in place to prevent the caller from uploading binary data.

The username, bucket, and key in the URL should be URL encoded.

Internally, the data is stored in Riak. The Riak bucket name is formed from a
combination of the username and the bucket name, such as "testuser-testbucket".
The content type stored in Riak will be "application/octet-stream"..

Buckets and keys are automatically created if they don't already exist.

Example:

```
$ curl -d "THIS-IS-A-TEST" "http://by-tor:8888/secured/buckets/ipctest/test-bucket/test-key?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true
}
```

Errors will return either a ERR_REQUESTED_FAILED or an ERR_UNCHECKED_EXCEPTION
error code with a 500 status code.


## Getting data from a user's bucket

Secured Endpoint: GET /secured/buckets/:username/:bucket/:key

Returns data associated with the key in the user's bucket. The body is returned
as a string. The data does NOT have to be JSON, it can be a normal string.

The username, bucket, and key in the URL should be URL encoded.

The data that is retrieved comes from Riak. The bucket is a combination of the
username and the bucket name, i.e. ipctest-testbuck. The content-type that is
returned will be "application/octet-stream".

Example:

```
$ curl "http://by-tor:8888/secured/buckets/ipctest/test-bucket/test-key?proxyToken=$(cas-ticket)" | python -mjson.tool
THIS-IS-A-TEST
```

Error will return either a ERR_REQUESTED_FAILED or an ERR_UNCHECKED_EXCEPTION
error code with a 500 status code.
