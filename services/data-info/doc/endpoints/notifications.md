# Table of Contents

* [Notification Endpoints](#notification-endpoints)
    * [Obtaining Notifications](#obtaining-notifications)
    * [Obtaining Notification Counts](#obtaining-notification-counts)
    * [Obtaining Unseen Notifications](#obtaining-unseen-notifications)
    * [Obtaining the Ten Most Recent Notifications](#obtaining-the-ten-most-recent-notifications)
    * [Marking Notifications as Seen](#marking-notifications-as-seen)
    * [Marking All Notifications as Seen](#marking-all-notifications-as-seen)
    * [Marking Notifications as Deleted](#marking-notifications-as-deleted)
    * [Marking All Notifications as Deleted](#marking-all-notifications-as-deleted)
    * [Sending an Arbitrary Notification](#sending-an-arbitrary-notification)
    * [Endpoints for System Messages (a.k.a. System Notifications)](#endpoints-for-system-messages-(a.k.a.-system-notifications))

# Notification Endpoints

## Obtaining Notifications

Secured Endpoint: GET /secured/notifications/messages

This endpoint is primarily a passthrough endpoint to the notification agent's
`/messages` endpoint, but it does make calls into metadactyl in order to add the
app description to job status update notifications.

Notifications in the DE are used to inform users when some event (for example a
job status change or the completion of a file upload) has occurred. This service
provides a way for the DE to retrieve notifications that the user may or may not
have seen before. This service accepts five different query-string parameters
(in addition to the `proxyToken` parameter, which is required for all secured
services):

<table>
    <thead>
        <tr><th>Name</th><th>Description</th><th>Required/Optional</th></tr>
    </thead>
    <tbody>
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
                the user has not seen, or both should be returned. If this
                parameter is equal to `true` then only messages that the user
                has seen will be returned. If it is equal to `false` then only
                messages that the user has not seen will be returned. If this
                parameter is not specified at all then both messages that have
                been seen and messages that have not been seen will be returned.
            </td>
            <td>Optional</td>
        </tr>
        <tr>
            <td>sortField</td>
            <td>
                The field to use when sorting messages. Currently, the only
                supported value for this field is `timestamp`.
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
                Specifies the type of notification messages to return, which can
                be `data`, `analysis` or `tool`. Other types of notifications
                may be added in the future. If this parameter it not specified
                then all types of notifications will be returned.
            </td>
            <td>Optional</td>
        </tr>
    </tbody>
</table>

The response body for this service is in the following format:

```json
{
    "messages": [
        {
            "deleted": "deleted-flag",
            "message": {
                "id": "message-id",
                "text": "message-text",
                "timestamp": "milliseconds-since-epoch",
            }
            "outputDir": "output-directory-path",
            "outputManifest": "list-of-output-files",
            "payload": {}
            "seen": "seen-flag",
            "type": "notification-type-code",
            "user": "username"
        },
    ],
    "total": "message-count"
}
```

The payload object in each message is a JSON object with a format that is
specific to the notification type, and its format will vary. There are currently
three types of notifications that we support: `data`, `analysis` and `tool`. The
`data` and `analysis` notification types have the same payload format:

```json
{
    "action": "action-code",
    "analysis-details": "analysis-description",
    "analysis_id": "analysis-id",
    "analyis_name": "analysis-name",
    "description": "job-description",
    "enddate": "end-date-in-milliseconds-since-epoch",
    "id": "job-id",
    "name": "job-name",
    "resultfolderid": "result-folder-path",
    "startdate": "start-date-in-milliseconds-since-epoch",
    "status": "job-status-code",
    "user": "username"
}
```

The payload format for the `tool` notification type is a little simpler:

```json
{
    "email_address": "email-address",
    "toolname": "tool-name",
    "tooldirectory": "tool-directory",
    "tooldescription": "tool-description",
    "toolattribution": "tool-attribution",
    "toolversion": "tool-version"
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/notifications/messages?proxyToken=$(cas-ticket)&limit=1&offset=0" | python -mjson.tool
{
    "messages": [
        {
            "deleted": false,
            "message": {
                "id": "C15763CF-A5C9-48F5-BE4F-9FB3CB1897EB",
                "text": "URL Import of somefile.txt from http://snow-dog.iplantcollaborative.org/somefile.txt completed",
                "timestamp": 1331068427000
            },
            "outputDir": "/iplant/home/nobody",
            "outputManifest": [],
            "payload": {
                "action": "job_status_change",
                "analysis-details": "",
                "analysis_id": "",
                "analysis_name": "",
                "description": "URL Import of somefile.txt from http://snow-dog.iplantcollaborative.org/somefile.txt",
                "enddate": 1331068427000,
                "id": "40115C19-AFBC-4CAE-9738-324DD8B18FDC",
                "name": "URL Import of somefile.txt from http://snow-dog.iplantcollaborative.org/somefile.txt",
                "resultfolderid": "/iplant/home/nobody",
                "startdate": "1331068414712",
                "status": "Completed",
                "user": "nobody"
            },
            "seen": true,
            "type": "data",
            "user": "nobody"
        }
    ],
    "total": "37"
}
```

## Obtaining Notification Counts

Secured Endpoint: GET /secured/notifications/count-messages

This endpoint is a passthrough to the notification agent endpoint,
`/count-messages`. Please see the notification agent documentation for more
details.

This service takes a subset of the query-string parameters as the /messages
service, and returns the number of messages that match the criteria specified in
the query-string parameters. Here's the list of supported query-string
parameters:

<table>
    <thead>
        <tr><th>Name</th><th>Description</th><th>Required/Optional</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>seen</td>
            <td>
                Indicates whether messages that the user has seen, messages that
                the user has not seen, or both should be counted. If this
                parameter is equal to `true` then only messages that the user
                has seen will be counted. If it is equal to `false` then only
                messages that the user has not seen will be counted. If this
                parameter is not specified at all then both messages that have
                been seen and messages that have not been seen will be counted.
            </td>
            <td>Optional</td>
        </tr>
        <tr>
            <td>filter</td>
            <td>
                Specifies the type of notification messages to return, which can
                be `data`, `analysis` or `tool`. Other types of notifications
                may be added in the future. If this parameter it not specified
                then all types of notifications will be returned.
            </td>
            <td>Optional</td>
        </tr>
    </tbody>
</table>

The response body consists of a JSON object containing four fields:
`user-total`, contains the number of user messages that have not been marked as
deleted and match the criteria specified in the query string, `system-total`
contains the number of system messages that are active and have not been
dismissed by the user, `system-total-new` contains the number of system messages
that have not been marked as received by the user, and `system-total-unseen`
contains the number of system messages that have not been marked as seen by the
user.

```json
{
    "user-total":          count,
    "system-total":        count,
    "system-total-new":    count,
    "system-total-unseen": count
}
```

Here are some examples:

```
$ curl -s "http://by-tor:8888/secured/notifications/count-messages?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "user-total":          409,
    "system-total":         10,
    "system-total-new":      0,
    "system-total-unseen":   1
}
```

In this example, all messages that are available for the user that have not been
marked as deleted are counted.

```
$ curl -s "http://by-tor:8888/secured/notifications/count-messages?proxyToken=$(cas-ticket)&filter=data" | python -mjson.tool
{
    "user-total":          91,
    "system-total":        10,
    "system-total-new":     0,
    "system-total-unseen":  1
}
```

In this example only the data notifications that have not been marked as deleted
are counted.

## Obtaining Unseen Notifications

Secured Endpoint: GET /secured/notifications/unseen-messages

This endpoint is primarily a passthrough endpoint to the notification agent's
`/unseen-messages` endpoint, but it does make calls into metadactyl in order to
add the app description to job status update notifications.

This service is used to obtain notifications that the user hasn't seen yet.
This service takes no query-string parameters other than the `proxyToken`
parameter that is required by all secured services. Here's an example:

```
$ curl -s "http://by-tor:8888/secured/notifications/unseen-messages?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "messages": []
}
```

## Obtaining the Ten Most Recent Notifications

Secured Endpoint: GET /secured/notifications/last-ten-messages

This endpoint is primarily a passthrough endpoint to the notification agent's
`/last-ten-messages` endpoint, but it does make calls into metadactyl in order
to add the app description to job status update notifications.

This endpoint returns the ten most recent messages for the authenticated user in
ascending order by message timestamp. Obtaining the ten most recent messages in
ascending order is difficult using other endpoints.

Examples are omitted for this endpoint because the response body is identical to
that of the other endpoints used to obtain notifications.

## Marking Notifications as Seen

Secured Endpoint: POST /secured/notifications/seen

This endpoint is a passthrough to the notification agent's `/seen`
endpoint. Please see the notification agent documentation for more details.

Once a user has seen a notification, the notification should be marked as seen
to prevent it from being returned by the `/notifications/unseen-messages`
endpoint. This service provides a way to mark notifications as seen. The request
body for this service is in the following format:

```
{
    "uuids": [
        "uuid-1",
        "uuid-2",
        "uuid-n"
    ]
}
```

The response body for this service is a simple JSON object that indicates
whether or not the service call succeeded and contains the number of messages
that are still marked as unseen. Here's an example:

```
$ curl -sd '
{
    "uuids": [
        "C15763CF-A5C9-48F5-BE4F-9FB3CB1897EB"
    ]
}
' "http://by-tor:8888/secured/notifications/seen?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true,
    "count": 0
}
```

Note that the UUIDs provided in the request body must be obtained from the
`message` -> `id` element of the notification the user wishes to mark as seen.

## Marking All Notifications as Seen

Secured Endpoint: POST /secured/notifications/mark-all-seen

This endpoint is a passthrough to the notification agent's `/mark-all-seen`
endpoint. Please see the notification agent documentation for more information
about the format of the request body.

This endpoint will add or overwrite the "user" field in the request body
forwarded to the NotificationAgent with the username of the authenticated user
making the request.

The response body for this service is a simple JSON object that indicates
whether or not the service call succeeded and contains the number of messages
that are still marked as unseen. Here's an example:

```
$ curl -sd '{}' "http://by-tor:8888/secured/notifications/mark-all-seen?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true,
    "count": 0
}
```

## Marking Notifications as Deleted

Secured Endpoint: POST /secured/notifications/delete

This endpoint is a passthrough to the notification agent's `/delete`
endpoint. Please see the notification agent documentation for more details.

Users may wish to dismiss notifications that they've already seen. This service
marks one or more notifications as deleted so that neither the
`/notfications/messages` endpoint nor the `/notifications/unseen-messages`
endpoint will return them. The request body for this service is in the following
format:

```
{
    "uuids": [
        "uuid-1",
        "uuid-2",
        "uuid-n"
    ]
}
```

The response body for this service is a simple JSON object that indicates
whether or not the service call succeeded. Here's an example:

```
$ curl -sd '
{
    "uuids": [
        "C15763CF-A5C9-48F5-BE4F-9FB3CB1897EB"
    ]
}
' "http://by-tor:8888/secured/notifications/delete?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true
}
```

Note that the UUIDs provided in the request body must be obtained from the
`message` -> `id` element of the notification the user wishes to delete.

## Marking All Notifications as Deleted

Secured Endpoint: DELETE /secured/notifications/delete-all

This endpoint is a passthrough to the notification agent's `/delete-all`
endpoint. Please see the notification agent documentation for more details.

This endpoint will add or replace the "user" parameter in the request forwarded
to the NotificationAgent with the username of the authenticated user making the
request.

The response body for this service is a simple JSON object that indicates
whether or not the service call succeeded and contains the number of messages
that are still marked as unseen. Here's an example:

```
$ curl -X DELETE -s "http://by-tor:8888/secured/notifications/delete-all?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true,
    "count": 0
}
```

## Sending an Arbitrary Notification

Unsecured Endpoint: POST /send-notification.

This endpoint is a passthrough to the notification agent's `/notification`
endpoint. Please see the notification agent documentation for more details.

## Endpoints for System Messages (a.k.a. System Notifications)

The endpoints for the system messages are straight pass throughs to the
corresponding calls in the Notification Agent. The only difference is that
the endpoints in data-info are prefixed with __/secured/notifications__ and that endpoints
that require the __user__ query string parameter instead take the __proxyToken__
parameter and its corresponding ticket string.

<table>
    <thead>
        <tr><th>data-info Endpoint</th><th>Notification Agent Endpoint</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>GET /secured/notifications/system/messages</td>
            <td>GET /system/messages</td>
        </tr>
        <tr>
            <td>GET /secured/notifications/system/new-messages</td>
            <td>GET /system/new-messages</td>
        </tr>
        <tr>
            <td>GET /secured/notifications/system/unseen-messages</td>
            <td>GET /system/unseen-messages</td>
        </tr>
        <tr>
            <td>POST /secured/notifications/system/received</td>
            <td>POST /system/received</td>
        </tr>
         <tr>
            <td>POST /secured/notifications/system/mark-all-received</td>
            <td>POST /system/mark-all-received</td>
        </tr>
        <tr>
            <td>POST /secured/notifications/system/seen</td>
            <td>POST /system/seen</td>
        </tr>
        <tr>
            <td>POST /secured/notifications/system/mark-all-seen</td>
            <td>POST /system/mark-all-seen</td>
        </tr>
        <tr>
            <td>POST /secured/notifications/system/delete</td>
            <td>POST /system/delete</td>
        </tr>
        <tr>
            <td>DELETE /secured/notifications/system/delete-all</td>
            <td>DELETE /system/delete-all</td>
        </tr>
        <tr>
            <td>PUT /secured/notifications/admin/system</td>
            <td>PUT /admin/system</td>
        </tr>
        <tr>
            <td>GET /secured/notifications/admin/system</td>
            <td>GET /admin/system</td>
        </tr>
        <tr>
            <td>GET /secured/notifications/admin/system/:uuid</td>
            <td>GET /admin/system/:uuid</td>
        </tr>
        <tr>
            <td>POST /secured/notifications/admin/system/:uuid</td>
            <td>POST /admin/system/:uuid</td>
        </tr>
        <tr>
            <td>DELETE /secured/notifications/admin/system/:uuid</td>
            <td>DELETE /admin/system/:uuid</td>
        </tr>
        <tr>
            <td>GET /secured/notifications/admin/system-types</td>
            <td>GET /admin/system-types</td>
        </tr>
    </tbody>
</table>
