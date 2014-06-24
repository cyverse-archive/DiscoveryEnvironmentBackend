# Table of Contents

* [Callback Endpoints](#callback-endpoints)
    * [Receiving DE Notifications](#receiving-de-notifications)
    * [Receiving Agave Job Status Updates](#receiving-agave-job-status-updates)
    * [Obtaining OAuth Authorization Codes](#obtaining-oauth-authorization-codes)

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

## Obtaining OAuth Authorization Codes

Secured Endpoint: GET /secured/oauth/access-code/{api-name}

The only API name that is currently supported is `agave`.

The DE calls this endpoint after the user gives the DE permission to access a
third-party API on his or her behalf. Agave sends a callback to a servlet in the
DE, which obtains a CAS proxy ticket and sends a request to this service to
obtain an access token for the user. This service requires the following query
parameters:

<table border="1">
    <thead>
        <tr><th>Parameter</th><th>Description</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>code</td>
            <td>The authorization code provided by the OAuth server.</td>
        </tr>
        <tr>
            <td>state</td>
            <td>A UUID referring to the original authorization request.</td>
        </tr>
    </tbody>
</table>

Upon success, the response to this service is in the following format:

```json
{
    "status": "success",
    "state_info": "{arbitrary-state-information}"
}
```

This service is used any time another service determines that an authorization
code is required in order to proceed. The steps are described in detail below:

### Step 1: The other service determines that authorization is required.

This can happen in two different ways:

1. No access token is found in the database for the user.
2. An access token is found, but attempts to use or refresh the token failed.

### Step 2: The other service stores state information in the database.

This state information is specific to each service, and enables the DE to route
the user back to the same window in the DE after the authorization process is
complete. The state information is associated with a UUID and the username of
the current user. This adds an extra layer of security because it allows this
service (the service that accepts the authorization code) to ensure that the
user who obtained the authorization code is the same as the user who initiated
the request that required it.

### Step 3: The user is redirected to the OAuth authorization service.

This redirection contains several query parameters that are either required or
recommended by the OAuth 2.0 specification:

<table border="1">
    <thead>
        <tr><th>Parameter</th><th>Description</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>response_type</td>
            <td>The type of response requested (always set to "code").</td>
        </tr>
        <tr>
            <td>client_id</td>
            <td>The DE's OAuth API client identifier.</td>
        </tr>
        <tr>
            <td>redirect_uri</td>
            <td>The URI to redirect the user to after authorization</td>
        </tr>
        <tr>
            <td>state</td>
            <td>The UUID associated with the state information from step 2.</td>
        </tr>
    </tbody>
</table>

### Step 4: The user either grants or denies authorization.

This step happens outside of the DE. The user authenticates to the external
service and either grants or denies the authorization request.

### Step 5: The user is redirected back to the DE.

After granting or denying authorization, the user is redirected to the URI
specified in the `redirect_uri` parameter described above. There are two
possibilities for this step: either the user granted or denied authorization.

If the user granted authorization then two query parameters will be included in
the request:

<table border="1">
    <thead>
        <tr><th>Parameter</th><th>Description</th><tr>
    </thead>
    <tbody>
        <tr>
            <td>code</td>
            <td>The authorization code provided by the OAuth server.</td>
        </tr>
        <tr>
            <td>state</td>
            <td>The same value that was sent to the authorization service.</td>
        </tr>
    </tbody>
</table>

In this case, a request is sent to Donkey so that an access token can be
obtained.

If the user denied authorization or an error occurred, then the response will
contain at least two and, possibly up to four query parameters:

<table border="1">
    <thead>
        <tr><th>Parameter</th><th>Required</th><Description</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>error</td>
            <td>Yes</td>
            <td>An error code indicating what went wrong.</td>
        </tr>
        <tr>
            <td>error_description</td>
            <td>No</td>
            <td>A human-readable description of the error.</td>
        </tr>
        <tr>
            <td>error_uri</td>
            <td>No</td>
            <td>A link to a human-readable description of the error.</td>
        </tr>
        <tr>
            <td>state</td>
            <td>Yes</td>
            <td>The same value that was sent to the authorization service.</td>
        </tr>
    </tbody>
</table>

In this case, the user is redirected back to the main page in the DE along with
some query parameters indicating that authorization was not obtained.

This request is handled by a servlet that fields requests to any URI path that
begins with the context path followed by `oauth/callback/`. There should be one
additional path element: the name of the API. For example, assuming that the
context path is `/de` and the API name is `agave`, the path in the request would
be `/de/oauth/callback/agave`.

### Step 6: A request is sent to the access-code service.

This step only occurs if the user granted authorization to the DE. The servlet
handling the redirection in the previous step extracts the API name from the URI
path and the authorization code and state identifier from the query
string.

The API name is used to construct the URI path for the service request. For
example, if the API name is `agave` then the URI path for the service request is
`/secured/oauth/access-code/agave`. The query string parameters are passed to
the service in the query string along with the usual CAS proxy token.

### Step 7: The state identifier is validated.

This step only occurs if the user granted authorization to the DE. The
`/secured/oauth/access-code/{api-name}` service obtains the user information
using the CAS proxy token and gets the state identifier from the query
string. The state information is then retrieved from the database using the
state identifier. If the state information isn't found or is associated with a
different user, the service responds with an error status. Otherwise, the state
information is removed from the database and the serivce proceeds to the next
step.

### Step 8: The service requests a token from the OAuth server.

This step only occurs if the user granted authorization to the DE. The
authorization code that was provided by the OAuth server is sent to the OAuth
access token endpoint. This request includes four query parameters:

<table border="1">
    <thead>
        <tr><th>Parameter</th><th>Description</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>grant_type</td>
            <td>Always set to "authorization_code".</td>
        </tr>
        <tr>
            <td>code</td>
            <td>The authorization code provided by the OAuth server.</td>
        </tr>
        <tr>
            <td>redirect_uri</td>
            <td>The same redirect URI sent to the authorization endpoint.</td>
        </tr>
        <tr>
            <td>client_id</td>
            <td>The API client identifier associated with the DE.</td>
        </tr>
    </tbody>
</table>

If the OAuth server responds with an error status then this service also
responds with an error status. Otherwise, the response will contain the access
token information.

### Step 9: The service stores the token information in the database.

This step only occurs if the user granted authorization and an access token was
successfully obtained.

The response from the OAuth server's token endpoint will contain the access
token, the number of seconds until the token expires, and a refresh token that
can be used to obtain a new access token when the current one expires. This
information is stored in the database associated with the API name and the
current user. After storing the token information, the service responds with a
JSON object containing the state information that was stored in the database.

### Step 10: The user is redirected back to the main page in the DE.

This step occurs whether or not authorization was obtained. If authorization was
obtained the state information from the service call is passed to the main DE
page in the query string. If authorization was not obtained then some query
parameters indicating what went wrong are included in the query string.
