# Notification Agent

This service accepts arbitrary notification requests from other DE back-end
services and provides endpoints for the Discovery Environment that are used to
look up the notifications for the current user. It also triggers the emails that
are sent when e-mail notifications are requested.

## Service Details

All service URLs are listed as relative URLs. The welcome endpoint, which can be
used to verify that the notification agent is running and responsive, accepts
GET requests with no query-string parameters.

### Verifying that the Notification Agent is Running

* Endpoint: GET /

The root path in the notification agent can be used to verify that the
notification agent is actually running. Sending a GET request to this service
will result in a welcome message being returned to the caller. Here's an
example:

```
$ curl http://by-tor:65533/
Welcome to the notification agent!
```

### Requesting an Arbitrary Notification

* Endpoint: POST /notification

The purpose of this endpoint is to allow other iPlant services to request
notifications to be sent to the user. The request body for this endpoint is an
abbreviated form of the notification format stored in the notification database.

```json
{
    "type": "some_notification_type",
    "user": "some_user_name",
    "subject": "some subject description",
    "message": "some message text",
    "email": true,
    "email_template": "some_template_name",
    "payload": {
        "email_address": "some@email.address"
    }
}
```

Only the `type`, `user` and `subject` fields are required. The `type` field
contains the notification type, which currently must be known to the UI. The UI
currently knows of three notification types `data`, `analysis` and `tool`. The
`user` field contains the user's unqualified username. (For example, if the full
username is `nobody@iplantcollaborative.org` then the short username is
`nobody`.)  The `subject` field contains a brief description of the event that
prompted the notification. The `message` field contains an optional description
of the event that prompted the notification. If this field is not provided then
its value will default to that of the `subject` field. The `email` field
contains a Boolean flag indicating whether or not an e-mail message should be
sent. The value of this field defaults to `false` if not provided. The
`email_template` field is required if an e-mail is requested, and it must
contain the name of an e-mail template that is known to the iplant-email
service. The payload is optional and may contain arbitrary information that may
be of use to any recipient of the notification. If an e-mail is requested then
this field must contain the user's e-mail address along with any information
required by the selected e-mail template. Here's an example:

```
curl -sd '
{
    "type": "nada",
    "user": "nobody",
    "subject": "nothing happened",
    "message": "nada y pues nada y pues nada",
    "email": true,
    "email_template": "nothing_happened",
    "payload": {
        "email_address": "nobody@iplantcollaborative.org"
    }
}
' http://by-tor:65533/notification | python -mjson.tool
{
    "success": true
}
```

Note that this example is fictional and will not actually send an e-mail message
because the requested e-mail template doesn't exist. The notification type is
also not known to the UI, which will cause errors in the UI.

If the service succeeds, a 200 status code is returned with a simple JSON
response body indicating that the service call succeeded. Otherwise, either a
400 or a 500 status code is returned and a brief description of the problem is
included in the response body.

### Getting Notifications from the Notification Agent

* Endpoint: GET /messages
* Endpoint: GET /unseen-messages

These two endpoints can be used to retrieve notifications from the notification
agent. The former can be used to get both notifications that have already been
seen and notifications that haven't been seen yet. It is very likely that each
user will have a lot of notifications that have already been seen, so the
`/messages` endpoint provides a paginated view, with both the number of messages
and the index of the starting message specified in the query string. The full
list of query string parameters for this endpoint is:

<table>
    <thead>
        <tr><th>Name</th><th>Description</th><th>Required/Optional</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>user</td>
            <td>The name of the user to retrieve notifications for.</td>
            <td>Required</td>
        </tr>
        <tr>
            <td>limit</td>
            <td>
                The maximum number of notifications to return at a time or `0`
                if there is no limit.
            </td>
            <td>Optional (defaults to `0`)</td>
        </tr>
        <tr>
            <td>offset</td>
            <td>The index of the starting message.</td>
            <td>Optional (defaults to `0`)</td>
        </tr>
        <tr>
            <td>seen</td>
            <td>
                Indicates whether messages that the user has seen, messages that
                the user has not seen, or both should be returned. If the value
                of this parameter is set to `true` then only messages that the
                user has seen will be returned. If the value of this parameter
                is `false` then only messages that the user has not seen will be
                returned. If the this parameter is not specified at all then
                both messages that the user has seen and messages that the user
                has not seen will be returned.
            </td>
            <td>Optional</td>
        </tr>
        <tr>
            <td>sortField</td>
            <td>
                The field to use when sorting messages. The values that are
                currently accepted for this field are: `date_created`,
                `timestamp` (which is, equivalent to `date_created`), `uuid` and
                `subject`.
            </td>
            <td>Optional (defaults to `timestamp`)</td>
        </tr>
        <tr>
            <td>sortDir</td>
            <td>
                The sorting direction, which can be `asc` (ascending) or `desc`
                (descending).
            </td>
            <td>Optional (defaults to `desc`)</td>
        </tr>
        <tr>
            <td>filter</td>
            <td>
                Specifies the type of notification messages to return, which
                can be `data`, `analysis`, `tool` or `new`. Other types of
                notifications may be added in the future. If this parameter
                it not specified then all types of notifications will be
                returned. The special filter type, `new`, is provided as a
                convenience for the UI. If this filter is specified then
                unseen notifications of any type will be returned.
            </td>
            <td>Optional</td>
        </tr>
    </tbody>
</table>

The `/unseen-messages` endpoint can only be used to obtain messages that haven't
been marked as seen yet. This service does not provide a paginated view because
it is likely that users will want to receive any notifications that they haven't
seen already immediately. This endpoint accepts only the `user` query-string
parameter.

Here are some examples:

```
$ curl -s 'http://by-tor:65533/messages?user=ipctest&limit=1&offset=0' | python -mjson.tool
{
    "messages": [
        {
            "deleted": false,
            "message": {
                "id": "6DF4475F-EE81-4063-B457-6EDFA4ED9C5F",
                "text": "cat_06221137 completed",
                "timestamp": 1340390304000
            },
            "outputDir": "/iplant/home/ipctest/analyses/cat_06221137-2012-06-22-11-37-58.636",
            "outputManifest": [],
            "payload": {
                "action": "job_status_change",
                "analysis_id": "a508674c3c9464ccbbbcf1600650db446",
                "analysis_name": "Concatenate Multiple Files",
                "description": "",
                "enddate": 1340390297000,
                "id": "jf408fc4d-628c-41bb-819c-4b5de9cb24e0",
                "name": "cat_06221137",
                "resultfolderid": "/iplant/home/ipctest/analyses/cat_06221137-2012-06-22-11-37-58.636",
                "startdate": "1340390278636",
                "status": "Completed",
                "user": "ipctest"
            },
            "seen": true,
            "type": "analysis",
            "user": "ipctest",
            "workspaceId": "39"
        }
    ],
    "total": "256"
}
```

```
$ curl -s 'http://by-tor:65533/unseen-messages?user=ipctest' | python -mjson.tool
{
    "messages": [],
    "total": "0"
}
```

### Getting the Ten Most Recent Notifications

* Endpoint: GET /last-ten-messages

This endpoint takes the username as its only query-string parameter and returns
the ten most recent messages for that username in ascending order by message
timestamp. Obtaining the ten most recent messages in ascending order is
difficult using other endpoints.

this endpoint takes one query-string parameter:

<table>
    <thead>
        <tr><th>Name</th><th>Description</th><th>Required/Optional</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>user</td>
            <td>The name of the user to retrieve notifications for.</td>
            <td>Required</td>
        </tr>
    </tbody>
</table>

Examples are omitted for this endpoint because the response body is identical to
that of the /messages and /unseen-messages endpoints.

### Counting Notifications

* Endpoint: GET /count-messages

This end-point returns count statistics for the messages that are currently
active. It counts for both system message counts and a user message count. For
the system messages, it returns the total number of messages, the total number
of unseen messages and the total number of new messages. The user messages may
be filtered by a set of criteria. Optional parameters provide this filtering.

This endpoint takes three query-string parameters:

<table>
    <thead>
        <tr><th>Name</th><th>Description</th><th>Required/Optional</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>user</td>
            <td>The name of the user to retrieve notifications for.</td>
            <td>Required</td>
        </tr>
        <tr>
            <td>seen</td>
            <td>
                Indicates whether messages that the user has seen, messages that
                the user has not seen, or both should be returned. If the value
                of this parameter is set to `true` then only messages that the
                user has seen will be returned. If the value of this parameter
                is `false` then only messages that the user has not seen will be
                returned. If the this parameter is not specified at all then
                both messages that the user has seen and messages that the user
                has not seen will be returned.
            </td>
            <td>Optional</td>
        </tr>
        <tr>
            <td>filter</td>
            <td>
                Specifies the type of notification messages to return, which
                can be `data`, `analysis` or `tool`. Other types of
                notifications may be added in the future. If this parameter
                it not specified then all types of notifications will be
                returned.
            </td>
            <td>Optional</td>
        </tr>
    </tbody>
</table>

The response body consists of a JSON object with a four fields:

```json
{
    "user-total":          count,
    "system-total":        count,
    "system-total-new":    count,
    "system-total-unseen": count
}
```

* `user-total` contains the number of user messages that match the criteria specified in the query string.
* `system-total` contains the number of system messages that are active and have not been dismissed by the user.
* `system-total-new` contains the number of system messages that have not been marked as received by the user.
* `system-total-unseen` contains the number of system message that have not been marked as seen by the user.

Here are some examples:

```
$ curl -s "http://by-tor:65533/count-messages?user=ipctest" | python -mjson.tool
{
    "user-total":          409,
    "system-total":         10,
    "system-total-new":      0,
    "system-total-unseen":   1
}
```

In this example, all user messages for the user, `ipctest`, are counted.

```
$ curl -s "http://by-tor:65533/count-messages?user=ipctest&filter=data" | python -mjson.tool
{
    "user-total":          91,
    "system-total":        10,
    "system-total-new":     0,
    "system-total-unseen":  1
}
```

In this example, all data messages for the user, `ipctest`, are counted.

```
$ curl -s "http://by-tor:65533/count-messages?user=ipctest&filter=data&seen=false" | python -mjson.tool
{
    "user-total":           0,
    "system-total":        10,
    "system-total-new":     0,
    "system-total-unseen":  1
}
```

In this example, only unseen data messages for the uer, `ipctest`, are counted.

### Marking Notifications as Seen

* Endpoint: POST /seen

Marking a notification as seen prevents it from being returned by the
`/unseen-messages` endpoint. The intent is for this endpoint to be called when
the user has seen a notification for the first time. This services requires a `user`
query parameter that provides the name of the user who is marking these messages as
seen. This service accepts a request body in the following format:

```json
{
    "uuids": [
        "some-uuid",
        "some-other-uuid",
        ...
    ]
}
```

If this service succeeds, it returns a 200 status code with a simple JSON
response body indicating that the service call suceeded along with the number of
messages that are still marked as unseen. Otherwise, it returns either a 400 or
a 500 status code with a brief description of the error. An attempt to mark a
non-existent message or a message that has already been marked as seen will be
silently ignored.

Here's an example:

```
$ curl -sd '
{
    "uuids": [
        "6DF4475F-EE81-4063-B457-6EDFA4ED9C5F"
    ]
}
' http://by-tor:65533/seen?user=ipctest | python -mjson.tool
{
    "success": true,
    "count": 0
}
```

### Marking All Notifications as Seen

* Endpoint: POST /mark-all-seen

This endpoint allows the client to acknowlege all notifications as seen for a
particular user. This service accepts a request body in a similar format as the
/notification endpoint. The only required JSON field is the "user" field, and
any additional fields will only mark notifications that match those fields as
seen:

```json
{
    "user": "some_user_name",
    ...
}
```

If this service succeeds, it returns a 200 status code with a simple JSON
response body indicating that the service call suceeded along with the number of
messages that are still marked as unseen. Otherwise, it returns either a 400 or
a 500 status code with a brief description of the error. An attempt to mark all
notifications seen for a non-existent user does not cause the service call to
fail. Here's an example:

```
$ curl -sd '
{
    "user": "some_user_name"
}
' http://by-tor:65533/mark-all-seen | python -mjson.tool
{
    "success": true,
    "count": 0
}
```

### Deleting Notifications

* Endpoint: POST /delete

"Deleting" a notification entails marking the notification as deleted in the
notification database so that it won't be returned by either the `/messages`
service or the `/unseen-messages` service. This service accepts a request body
in the following format:

```json
{
    "uuids": [
        "some-uuid",
        "some-other-uuid",
        ...
    ]
}
```

If this service succeeds it returns a 200 status code with a simple JSON
response body indicating that the service call succeeded. Otherwise, it returns
either a 400 status code or a 500 status code with a brief description of the
error. An attempt to delete a message that has already been marked as deleted
does not result in an error. Instead, the service just treats the request as a
no-op. Similarly, an attempt to delete a non-existent message is not treated as
an error. The service also treats this condition as a no-op. Here's an example:

```
$ curl -sd '
{
    "uuids": [
        "361C2A67-3942-4F7A-9734-E5B8B28FDC12"
    ]
}
' http://by-tor:65533/delete?user=ipctest | python -mjson.tool
{
    "success": true
}
```

### Deleting All Notifications

* Endpoint: DELETE /delete-all

This endpoint allows the client to delete all notifications for a particular
user. This service accepts query parameters similar to the fields accepted by
the /notification endpoint. The only required parameter is the "user" parameter,
and any additional parameters will only delete notifications that match those
parameters.

If this service succeeds it returns a 200 status code with a simple JSON
response body indicating that the service call succeeded. Otherwise, it returns
either a 400 status code or a 500 status code with a brief description of the
error. Here's an example:

```
$ curl -X DELETE -s 'http://by-tor:65533/delete-all?user=ipctest' | python -mjson.tool
{
    "success": true,
    "count": "0"
}
```

### Admin - Adding A System Notification

* Endpoint PUT /admin/system

This endpoint allows a client to add a new system notification. There are no query
parameters, only JSON contained in the request body.

This service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The HTTP method used for the request should be a PUT. The request body should be
JSON that looks something like the following:


    {
      "type" : "warning",
      "message" : "This is a warning",
      "deactivation_date" : 1340390297000,
      "activation_date" : 1340390297000,
      "dismissible" : false,
      "logins_disabled" : false
    }


The 'type', 'message', and 'deactivation_date' keys are required. The rest are
optional.

The response body for a successful addition will look something like the following:

    {
      "action" : "add-system-notification",
      "success": true,
      "system-notification" : {
        "activation_date": 1340390297000,
        "date_created": 1340390297000,
        "deactivation_date": 1340390297000,
        "dismissible": false,
        "logins_disabled": false,
        "message": "This is a warning",
        "type": "warning",
        "uuid": "140ee541-9967-47cd-ba2b-3b17d8c19dae"
      }
    }

Sample curl:

    curl -X PUT -d '<JSON from the above example>' http://127.0.0.1:31320/admin/system

### Admin - Listing System Notifications

* Endpoint GET /admin/system

This endpoint allows a client to list system notifications that match criteria
specified in the query string. This endpoint currently accepts four query string
parameters:

<table>
    <thead>
        <tr><th>Parameter</th><th>Description</th><th>Required/Optional</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>active-only</td>
            <td>
                If this parameter is specified and set to `true` then only
                system messages that are currently active will be returned by
                the service. Otherwise, all system messages will be returned,
                active or not.
            </td>
            <td>Optional (defaults to `false`)</td>
        </tr>
        <tr>
            <td>type</td>
            <td>
                If this parameter is specified then only system messages of
                the specified type will be returned by the service. The
                `/admin/system-types` endpoint can be used to obtain a list
                of valid system message types. Note that no validation is
                performed on this argument. If an invalid system message type
                is specified then the service will return an empty result set.
            </td>
            <td>Optional (all types of messages are returned by default)</td>
        </tr>
        <tr>
            <td>limit</td>
            <td>
                The maximum number of results to return at a time or 0 if there
                is no limit.
            </td>
            <td>Optional (defaults to 0)</td>
        </tr>
        <tr>
            <td>offset</td>
            <td>The index of the starting message.</td>
            <td>Optional (defaults to 0)</td>
        </tr>
    </tbody>
</table>

All query string arguments are optional.

Here's an example of a successful listing:

```
$ curl -s "http://by-tor:65533/admin/system?active-only=true&type=announcement&limit=1" | python -mjson.tool
{
    "action": "admin-list-system-notifications",
    "status": "success",
    "system-messages": [
        {
            "activation_date": "1377211511344",
            "date_created": "1377211511352",
            "deactivation_date": "1388559599000",
            "dismissible": false,
            "logins_disabled": false,
            "message": "foo",
            "type": "announcement",
            "uuid": "48c39ff1-8799-4f5d-a79f-9d228c658daa"
        }
    ],
    "total": 3
}
```

### Admin - Getting a System Notification by UUID

* Endpoint GET /admin/system/<uuid>

This endpoint allows a client to get information related to a specific system notification.
There are no query parameters or request bodies associated with this request.

This service returns a 200 status code with a JSON response body. Otherwise, it returns
either a 400 or 500 status code with a description of the error.

The HTTP method used for the request should be a GET.

The response body for a successful lookup will look something like the following:

    {
      "action" : "get-system-notification",
      "success": true,
      "system-notification" : {
        "activation_date": 1340390297000,
        "date_created": 1340390297000,
        "deactivation_date": 1340390297000,
        "dismissible": false,
        "logins_disabled": false,
        "message": "This is a warning",
        "type": "warning",
        "uuid": "140ee541-9967-47cd-ba2b-3b17d8c19dae"
      }
    }

Sample curl:

    curl http://127.0.0.1:31320/admin/system/140ee541-9967-47cd-ba2b-3b17d8c19daec

### Admin - Updating A System Notification

* Endpoint POST /admin/system/<uuid>

This endpoint allows a client to update an existing system notification. There are
no query parameters, only JSON contained in the request body.

This service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The HTTP method used for the request should be a POST. The request body should be
JSON that looks something like the following:

    {
      "action" : "update-system-notification",
      "success" : true,
      "system-notification" : {
        "type" : "warning",
        "message" : "This is a warning",
        "deactivation_date" : 1340390297000,
        "activation_date" : 1340390297000,
        "dismissible" : false,
        "logins_disabled" : false
      }
    }

All keys are optional.

The response body for a successful update will look something like the following:

    {
      "activation_date": 1340390297000,
      "date_created": 1340390297000,
      "deactivation_date": 1340390297000,
      "dismissible": false,
      "logins_disabled": false,
      "message": "This is a warning",
      "success": true,
      "type": "warning",
      "uuid": "140ee541-9967-47cd-ba2b-3b17d8c19dae"
    }

Sample curl:

    curl -X POST -d '<JSON from the above example>' http://127.0.0.1:31320/admin/system/<uuid>

### Admin - Deleting a System Notification by UUID

* Endpoint DELETE /admin/system/<uuid>

This endpoint allows a client to delete a specific system notification.
There are no query parameters or request bodies associated with this request.

This service returns a 200 status code with a JSON response body. Otherwise, it returns
either a 400 or 500 status code with a description of the error.

The HTTP method used for the request should be a DELETE.

The response body for a successful deletion will look something like the following:

    {
      "action" : "delete-system-notification",
      "success" : true,
      "system-notification" : {
        "activation_date": 1340390297000,
        "date_created": 1340390297000,
        "deactivation_date": 1340390297000,
        "dismissible": false,
        "logins_disabled": false,
        "message": "This is a warning",
        "type": "warning",
        "uuid": "140ee541-9967-47cd-ba2b-3b17d8c19dae"
      }
    }

Sample curl:

    curl -X DELETE http://127.0.0.1:31320/admin/system/140ee541-9967-47cd-ba2b-3b17d8c19daec

### Admin - Getting All System Notification Types

* Endpoint: GET /admin/system-types

This endpoint allows a client to request a list of all system notification types.
There are no query parameters or request bodies associated with this request.

The service returns a 200 status code with a JSON response body. Otherwise, it returns
either a 400 or 500 status code with a description of the error.

The HTTP method used for the request is GET.

The response body for a successful lookup will look something like the following:

    {
      "action" : "get-system-notification-types",
      "status" : "success",
      "types" : [
          "announcement",
          "maintenance",
          "warning"
      ]
    }

Sample curl:

    curl http://127.0.0.1:31320/admin/system-types

### Getting All System Notifications For A User

* Endpoint: GET /system/messages

This endpoint allows a client to request a list of all system notifications that
are currently relevant for a user, regardless of whether they have been seen or
not. This endpoint will not list system notifications that have been "soft"
deleted (also known as "dismissed") by the user.

This endpoint accepts a `user` query parameter that tells the service which
user's system notifications to return.

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The HTTP method used for the request is a GET.

The response body for a successful lookup will look like the following:

```json
{
  "system-messages" : [
    {
      "deactivation_date" : 1340390297000,
      "dismissible" : true,
      "activation_date" : 1340390297000,
      "date_created" : 1340390297000,
      "uuid" : "933ab627-5d1e-4cd9-b6e8-ff5462243637",
      "type" : "warning",
      "message" : "I like cake",
      "acknowledged" : false,
      "logins_disabled" : false
    }
  ]
}
```

Sample curl:

    curl http://127.0.0.1:31320/system/messages?user=wregglej

### Getting Only New System Notifications For A User

* Endpoint: GET /system/new-messages

This endpoint allows a client to request a list of only the new system
notifications that are currently relevant for a user.

This endpoint accepts a `user` query parameter that tells the service which
user's system notifications to return.

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The HTTP method used for the request is a GET.

The response body for a successful lookup with look like the following:

```json
{
  "system-messages" : [
    {
      "deactivation_date" : "1227582343552",
      "dismissible" : true,
      "activation_date" : "1227582343552",
      "date_created" : "1227582343552",
      "uuid" : "933ab627-5d1e-4cd9-b6e8-ff5462243637",
      "type" : "warning",
      "message" : "I like cake",
      "acknowledged" : false,
      "logins_disabled" : false
    }
  ]
}
```

The date fields are all represented as milliseconds since the epoch.

Sample curl:

    curl http://127.0.0.1:31320/system/new-messages?user=wregglej

### Getting All Unseen System Notifications For A User

* Endpoint: GET /system/unseen-messages

This endpoint allows a client to request a list of all system notifications that
are currently unseen for a user. This endpoint will not list system notifications
that have been "soft" deleted (also known as "dismissed") by the user. Nor will
it list system notifications that have already been seen by the user, regardless
of whether or not the message has been dismissed or is marked as dismissible.

This endpoint accepts a `user` query parameter that tells the service which
user's system notifications to return.

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The HTTP method used for the request is a GET.

The response body for a successful lookup with look like the following:

```json
{
  "system-messages" : [
    {
      "deactivation_date" : 1340390297000,
      "dismissible" : true,
      "activation_date" : 1340390297000,
      "date_created" : 1340390297000,
      "uuid" : "933ab627-5d1e-4cd9-b6e8-ff5462243637",
      "type" : "warning",
      "message" : "I like cake",
      "acknowledged" : true,
      "logins_disabled" : false
    }
  ]
}
```

Sample curl:

    curl http://127.0.0.1:31320/system/unseen-messages?user=wregglej

### Marking System Notifications As Received By A User

* Endpoint: POST /system/received

This endpoint allows a client to specify a list of system notifications that
should be marked as received by the specified user.

This endpoint accepts a `user` query parameter that tells the service which
user's system notifications to return.

The HTTP Method used for the request is a POST.

The body of the request should be JSON that looks like the following:

```json
{
  "uuids" : [
    "23705cb9-2a46-4cc3-80de-989d86ecbd01",
    "933ab627-5d1e-4cd9-b6e8-ff5462243637"
  ]
}
```

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The response body for a successful marking will look like the following:

```json
{
  "success" : true,
  "count" : "1"
}
```

The `count` field contains the number of system messages that are still
unreceived by the user.

Passing the same UUIDs in to the endpoint multiple times is simply a no-op and
does not return any errors.

Sample curl:

```
curl -d '{"uuids" : ["23705cb9-2a46-4cc3-80de-989d86ecbd01", "933ab627-5d1e-4cd9-b6e8-ff5462243637"]}' http://127.0.0.1:31320/system/received?user=wregglej
```

### Marking All System Notifications As Received By A User

* Endpoint: POST /system/mark-all-received

This endpoint allows a client to mark all of the applicable system notifications
as received by a user.

The HTTP Method used for the request is a POST.

This endpoint has no query parameters, but accepts a JSON-encoded request body
that should look something like the following:

```json
{
  "user" : "wregglej"
}
```

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The response body for a successful lookup with look like the following:

```json
{
  "success" : true,
  "count" : "0"
}
```

The `count` field contains the number of system messages that are still
unreceived by the user.

Sample curl:

```
curl -d '{"user" : "wregglej"}' http://127.0.0.1:31320/system/mark-all-received
```

### Marking System Notifications As Seen By A User

* Endpoint: POST /system/seen

This endpoint allows a client to specify a list of system notifications that
should be marked as seen by the specified user. Any system notification may be
marked as seen by a user, even system notifications that cannot be completely
dismissed by the user.

This endpoint accepts a `user` query parameter that tells the service which
user's system notifications to return.

The HTTP Method used for the request is a POST.

The body of the request should be JSON that looks like the following:

    {
      "uuids" : [
        "23705cb9-2a46-4cc3-80de-989d86ecbd01",
        "933ab627-5d1e-4cd9-b6e8-ff5462243637"
      ]
    }

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The response body for a successful marking will look like the following:

    {
      "success" : true,
      "count" : "2"
    }

The `count` field contains the number of system messages that are still unseen
by the user.

Passing the same UUIDs in to the endpoint multiple times is simply a no-op and
does not return any errors.

Sample curl:

    curl -d '{"uuids" : ["23705cb9-2a46-4cc3-80de-989d86ecbd01", "933ab627-5d1e-4cd9-b6e8-ff5462243637"]}' http://127.0.0.1:31320/system/seen?user=wregglej

### Marking All System Notifications As Seen By A User

* Endpoint: POST /system/mark-all-seen

This endpoint allows a client to mark all of the applicable system notifications
as seen by a user. Any system notification can be marked as seen by the user,
even those that are marked as not dismissible.

The HTTP Method used for the request is a POST.

This endpoint has no query parameters, but accepts a JSON-encoded request body
that should look something like the following:

    {
      "user" : "wregglej"
    }

The service returns a 200 status code with a JSON response body. Otherwise, it
returns either a 400 or 500 status code with a description of the error.

The response body for a successful lookup with look like the following:

    {
      "success" : true,
      "count" : "2"
    }

The `count` field contains the number of system messages that are still unseen
by the user.

Sample curl:

    curl -d '{"user" : "wregglej"}' http://127.0.0.1:31320/system/mark-all-seen

### Soft Deleting System Notifications For A User

* Endpoint: POST /system/delete

This endpoint allows a client to soft delete a batch of system notifications
for a user. This does not remove them from the database, but will prevent
the system notification from showing up in any of the non-admin endpoints
that return system notifications for a user. This does not apply to system
notifications that have "dismissible" set to false, which is the default.
Also, any notifications that are marked as deleted by a user are also marked
as seen by the user.

This endpoint accepts a "user" query parameter that tells the service which
user's system notifications to delete.

The HTTP method used for the request is POST.

The body of the request should be JSON that looks like the following:

    {
      "uuids" : [
        "23705cb9-2a46-4cc3-80de-989d86ecbd01",
        "933ab627-5d1e-4cd9-b6e8-ff5462243637"
      ]
    }

The response body for a successful deletion with look like the following:

    {
      "success" : true,
      "count" : "2"
    }

The count field tells the number of applicable system notifications for
the user that made the request, regardless of whether the notifications
have been seen or not.

Sample curl:

    curl -d '{"uuids" : ["23705cb9-2a46-4cc3-80de-989d86ecbd01", "933ab627-5d1e-4cd9-b6e8-ff5462243637"]}' http://127.0.0.1:31320/system/delete?user=wregglej

### Soft Deleting All System Notifications For A User

* Endpoint: DELETE /system/delete-all

This endpoint allows a client to soft delete all system notifications
for a user. This does not remove them from the database, but will prevent
the system notifications from showing up in any of the non-admin endpoints
that return system notifications for a user. This does not apply to system
notifications that have "dismissible" set to false, which is the default.
Also, any notifications that are marked as deleted by a user are also marked
as seen by the user.

This endpoint accepts a "user" query parameter that tells the service which
user's system notifications to delete.

The HTTP method used for the request is DELETE.

The response body for a successful deletion with look like the following:

    {
      "success" : true,
      "count" : "2"
    }

The count field tells the number of applicable system notifications for
the user that made the request, regardless of whether the notifications
have been seen or not.

Sample curl:

    curl -X DELETE http://127.0.0.1:31320/system/delete-all?user=wregglej

### Unrecognized Service Path

If the notification agent doesn't recognize a service path then it will respond
with a 400 status code along with a message indicating that the service path is
not recognized. Here's an example:

```
dennis$ curl -s http://by-tor:65533/foo
Unrecognized service path.
```
