# Table of Contents

* [Callback Endpoints](#callback-endpoints)
    * [Receiving DE Notifications](#receiving-de-notifications)
    * [Receiving Agave Job Status Updates](#receiving-agave-job-status-updates)

# Callback Endpoints

These endpoints listed in this document accept callbacks from other services
indicating that some event has occurred.

## Receiving DE Notifications

Unsecured Endpoint: POST /callbacks/notification

This endpoint accepts notifications from the notification agent. The request
body for this endpoint is a notification in the same format as returned by the
notification agent's `/messages` endpoint. Here's an example:

```
$ curl -sd '{
    "deleted": false,
    "message": {
        "id": "DA357AC8-C311-44AD-BA79-23C9AF73850D",
        "text": "wc_10081655 submitted",
        "timestamp": "1381276614133"
    },
    "outputDir": "/iplant/home/snow-dog/analyses/wc_10081655-2013-10-08-16-56-53.284",
    "outputManifest": [],
    "payload": {
        "action": "job_status_change",
        "analysis_id": "C7F05682-23C8-4182-B9A2-E09650A5F49B",
        "analysis_name": "Word Count",
        "description": "",
        "display_name": "",
        "enddate": "",
        "id": "29E3A5C8-EAD4-4F79-B450-A2FEF6548C30",
        "name": "wc_10081655",
        "resultfolderid": "/iplant/home/snow-dog/analyses/wc_10081655-2013-10-08-16-56-53.284",
        "startdate": "1381276613284",
        "status": "Submitted",
        "user": "snow-dog"
    },
    "seen": true,
    "type": "analysis",
    "user": "snow-dog"
}
' http://by-tor:8888/callbacks/notification | python -mjson.tool
{
    "success": true
}
```

This service currently ignores any notifications that are not job status update
notifications. When it receives a job status update notification, it stores
updated information about the job status in the database.

## Receiving Agave Job Status Updates

Unsecured Endpoint: POST /callbacks/agave-job/{job-uuid}

Understanding this endpoint requires a bit of background information. Submitting
jobs to Agave with a callback request poses kind of a chicken-and-egg problem
for the DE. The DE needs to be able to associate each specific callback URL with
a single Agave job. The easiest way to do that is to place the job identifier in
the URL so that we can use it to look up the local job status information. In
order for Agave to perform callbacks, however, it's necessary to send the
callback URL in the job request and the DE doesn't know the Agave job ID until
it gets the response from the job submission request.

The DE gets around this problem by associating every job with both an internal
ID and an external ID. Either the internal ID or the external ID can be used to
look up a job status record in the database. The UUID in the callback URL is the
internal ID in the DE database.

The status update notifications from Agave are fundamentally different from the
ones sent by the notification agent. A callback from Agave only indicates _that_
the job status has changed; it doesn't provide information about _how_ the job
status changed. For that, the DE has to contact Agave in order to retrieve the
current job status. For this reason, this service ignores the request body.
Agave does send a request body for this service call, a URL-encoded form
containing a single success indicator, but the request body isn't required.

When this endpoint receives a request, it first looks up the UUID in the
database. Assuming a corresponding job was found, the service then retrieves the
external job identifier (that is, the Agave job identifier) from the job record
and uses it to retrieve the job status information from Agave. Assuming that the
information is successfully retrieved from Agave, the status information is
extracted and the job record in the database is updated.

Here's an example of a successful service call:

```
$ curl -sd '' http://by-tor:8888/callbacks/agave-job/bd4c266f-11db-475b-a359-d667593b5906 | python -mjson.tool
{
    "success": true
}
```

This service will fail if the given UUID can't be found or if the Agave job
associated with the UUID can't be retrieved. Here's an example of the case where
the UUID can't be found:

```
$ curl -sd '' http://by-tor:8888/callbacks/agave-job/bd4c266f-11db-475b-a359-d667593b5905 | python -mjson.tool
{
    "error_code": "ERR_NOT_FOUND",
    "message": "job bd4c266f-11db-475b-a359-d667593b5905 not found",
    "success": false
}
```

Here's an example of the case where the corresponding Agave job isn't readable
by the user that's associated with the job in the database:

```
$ curl -sd '' http://by-tor:8888/callbacks/agave-job/bd4c266f-11db-475b-a359-d667593b5906 | python -mjson.tool
{
    "error_code": "ERR_REQUEST_FAILED",
    "message": "lookup for HPC job 30900",
    "success": false
}
```

Here's an example of the case where the Agave job can't be found:

```
$ curl -sd '' http://bt-tor:31325/callbacks/agave-job/bd4c266f-11db-475b-a359-d667593b5906 | python -mjson.tool
{
    "error_code": "ERR_NOT_FOUND",
    "message": "HPC job 99999 not found",
    "success": false
}
```
