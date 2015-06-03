This document describes the comments resource.

A _comment_ is something that a user records about a given app, file, or folder.

# Comment Endpoints

## Creating a comment

`POST /apps/{app-id}/comments`

Delegates to metadata: `POST /apps/{app-id}/comments`

`POST /secured/filesystem/entry/{entry-id}/comments`

Delegates to metadata: `POST /filesystem/entry/{entry-id}/comments`

This endpoint allows an authenticated user to post a comment on any app, accessible file, or
accessible folder.
`{app-id}` is the UUID of the app being commented on.
`{entry-id}` is the UUID of the file or folder being commented on.

These endpoints forward requests to their corresponding metadata service endpoints.
Please see the metadata documentation for more information.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the comment is determined from the authentication.  Any additional parameters
will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 201         | The comment was successfully posted |
| 400         | The request body wasn't syntactically correct |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | From the `/apps/{app-id}/comments` endpoint, the `app-id` didn't correspond to an existing app. From the `/secured/filesystem/entry/{entry-id}/comments` endpoint, `entry-id` didn't correspond to an existing file or folder, or is not accessible by the user |
| 413         | The request body is too large |

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl -X POST -d '{ "comment" : "That was interesting." }' localhost/secured/filesystem/entry/f86700ac-df88-11e3-bf3b-6abdce5a08d5/comments?proxyToken=fake-token
```
```json
{
  "comment" : {
    "id"        : "68a6a1f0-f745-11e3-b125-73dece2a6978",
    "commenter" : "tedgin",
    "post_time" : 1403093431927,
    "retracted" : false,
    "comment"   : "That was interesting."
  }
}
```

## Listing comments

`GET /apps/{app-id}/comments`

Delegates to metadata: `GET /apps/{app-id}/comments`

`GET /secured/filesystem/entry/{entry-id}/comments`

Delegates to metadata: `GET /filesystem/entry/{entry-id}/comments`

This endpoint allows an authenticated user to retrieve all of the comments made on an accessible
file or folder, or any app.
`{app-id}` is the UUID associated with the app.
`entry-id` is the UUID associated with the file or folder.

These endpoints forward requests to their corresponding metadata service endpoints.
Please see the metadata documentation for more information.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the comment is determined from the authentication.  Any additional parameters
will be ignored.

Any attached request body will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The list of comments will be in the response body |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | From the `/apps/{app-id}/comments` endpoint, the `app-id` didn't correspond to an existing app. From the `/secured/filesystem/entry/{entry-id}/comments` endpoint, `entry-id` didn't correspond to an existing file or folder, or is not accessible by the user |

Upon failure, a JSON document with a `reason` field will the returned. The `reason` field will
provide a short, human readable explanation of the failure.

### Example

```
? curl localhost/secured/filesystem/entry/f86700ac-df88-11e3-bf3b-6abdce5a08d5/comments?proxyToken=fake-token
```
```json
{
  "comments" : [
    {
      "id"        : "68a6a1f0-f745-11e3-b125-73dece2a6978",
      "commenter" : "tedgin",
      "post_time" : 1403093431927,
      "retracted" : false,
      "comment"   : "That was interesting."
    },
    {
      "id"        : "79a6a1f0-0745-21e3-c125-73dece2a6989",
      "commenter" : "fool",
      "post_time" : 1403093433001,
      "retracted" : false,
      "comment"   : "No it wasn't."
    }
  ]
}
```

## Retracting/Readmitting a comment

`PATCH /apps/{app-id}/comments/{comment-id}`

`PATCH /secured/filesystem/entry/{entry-id}/comments/{comment-id}`

Delegates to metadata:

`PATCH /apps/{app-id}/comments/{comment-id}`

`PATCH /admin/apps/{app-id}/comments/{comment-id}`

`PATCH /filesystem/entry/{entry-id}/comments/{comment-id}`

`PATCH /admin/filesystem/entry/{entry-id}/comments/{comment-id}`

These endpoints allow an authenticated user to retract a given comment or to readmit a retracted
comment on a given app, file, or folder.
`app-id` is the UUID of the app.
`entry-id` is the UUID of the file or folder.
`comment-id` is the UUID of the comment.

These endpoints forward requests to their corresponding metadata service endpoints.
In the case that the user is the owner of the app, file, or folder, then the request is forwarded to
the corresponding metadata service `/admin` endpoint.
Please see the metadata documentation for more information.

### Request

In addition to the `proxyToken` authentication parameter, this endpoint requires a boolean
`retracted` parameter.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The comment corresponding to the `comment-id` UUID has been marked as retracted. |
| 400         | The `retracted` parameter was missing or had a value other than `true` or `false`. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 403         | See [403](#403). |
| 404         | `comment-id` doesn't exist, `app-id` doesn't exist, or `entry-id` doesn't exist or isn't accessible by the user.
| 409         | One of the query parameters was passed more than once with different values. |

Error responses may include a `reason` field, providing a short, human readable explanation of the
failure.

#### 403

When a comment is being retracted, a 403 will result if the app, file, or folder is not owned by the
user and the comment was not created by the user. When a comment is being readmitted, a 403 will
result if the comment wasn't originally retracted by the user.

### Example

```
curl -X PATCH "localhost/secured/filesystem/f86700ac-df88-11e3-bf3b-6abdce5a08d5/comments/79a6a1f0-0745-21e3-c125-73dece2a6989?proxyToken=fake-token&retracted=true"
```

## Administratively deleting a comment

`DELETE /admin/apps/{app-id}/comments/{comment-id}`

Delegates to metadata: `DELETE /admin/apps/{app-id}/comments/{comment-id}`

`DELETE /admin/filesystem/entry/{entry-id}/comments/{comment-id}`

Delegates to metadata: `DELETE /admin/filesystem/entry/{entry-id}/comments/{comment-id}`

This endpoint allows an administrative user to delete a given comment on a given app, file, or
folder.
`comment-id` is the UUID of the comment.
`app-id` is the UUID of the app.
`entry-id` is the UUID of the file or folder.

These endpoints forward requests to their corresponding metadata service endpoints.
Please see the metadata documentation for more information.

### Request

Only the `proxyToken` authentication parameter is required. All other parameters are ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The comment corresponding to the `comment-id` UUID has been deleted. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | Either the app corresponding to `app-id` doesn't exist, the file or folder corresponding to `entry-id` doesn't exist, or the comment corresponding to `comment-id` doesn't. |

Error responses may include a `reason` field, providing a short, human readable explanation of the
failure.

### Example

```
curl -X DELETE "localhost/admin/filesystem/entry/f86700ac-df88-11e3-bf3b-6abdce5a08d5/comments/79a6a1f0-0745-21e3-c125-73dece2a6989?proxyToken=fake-token"
```
