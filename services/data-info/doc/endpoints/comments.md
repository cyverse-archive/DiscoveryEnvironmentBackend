This document describes the comments resource.

A _comment_ is something that a user records about a given file or folder.

# Resources

## Comment

A comment is modeled as a JSON document (media type `application/json`) with the fields described in
the following table.

| Field     | Type    | Description |
| --------- | ------- | ----------- |
| id        | string  | the service-provided UUID associated with the comment |
| commenter | string  | the authenticated username of the person who made the comment |
| post_time | number  | the time when the comment was posted in ms since the POSIX epoch |
| retraced  | boolean | a flag indicating whether or no the comment is currently retracted |
| comment   | string  | the text of the comment |

# Endpoints

## Creating a comment

`POST /secured/filesystem/entry/{entry-id}/comments`

This endpoint allows an authenticated user to post a comment on any accessible file or folder.
`{entry-id}` is the UUID of the file or folder being commented on.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the comment is determined from the authentication.  Any additional parameters
will be ignored.

The request body will be a JSON document (media type `application/json`) containing a `comment`
field. The user's comment should be the value of this field.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 201         | The comment was successfully posted |
| 400         | The request body wasn't syntactically correct |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | `entry-id` didn't correspond to an existing file or folder, or is not accessible by the user |
| 413         | The request body is too large |

Upon success, the response body will be a JSON document (media type `application/json`) with two
fields. The `comment` field will contain the corresponding [comment](#comment) object, and the
`success` field will have the value `true`.

Upon failure, a JSON document with `success` and `reason` fields will the returned. The `success`
field will have the value `false`.  The `reason` field will provide a short, human readable
explanation of the failure.

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
  },
  "success" : true
}
```

## Listing comments

`GET /secured/filesystem/entry/{entry-id}/comments`

This endpoint allows an authenticated user to retrieve all of the comments made on an accessible
file or folder. `entry-id` is the UUID associated with the file or folder.

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
| 404         | `entry-id` didn't correspond to an existing file or folder, or is not accessible by the user |

Upon success, the response body will be a JSON document (media type `application/json`) with two
fields. The `comments` field will contain an array of [comment](#comment) objects, and the `success`
field will have the value `true`.

Upon failure, a JSON document with `success` and `reason` fields will the returned. The `success`
field will have the value `false`.  The `reason` field will provide a short, human readable
explanation of the failure.

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
  ],
  "success" : true
}
```

## Retracting/Readmitting a comment

`PATCH /secured/filesystem/entry/{entry-id}/comments/{comment-id}`

This endpoint allows an authenticated user to retract a given commment on a given file or folder.
`entry-id` is the UUID of the file or folder, and `comment-id` is the UUID of the comment. The user
must either be an owner of the file or folder, or the user must be the commenter.

This endpoint also allows an authenticated user to readmit a retracted comment. Again, `entry-id` is
the UUID of the file or folder, and `comment-id` is the UUID of the comment. The user must be the
same user who retracted the comment.

### Request

In addition to the the `proxyToken` authentication parameter, this endpoint requires a boolean
`retracted` parameter.  A value of `true` indicates that the comment is being retracted, while a
value of `false` indicates the comment is being readmitted.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The comment corresponding to the `comment-id` UUID has been marked as a retracted. |
| 400         | The `retracted` parameter was missing or had a value other than `true` or `false`. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 403         | See [403](#403). |
| 404         | `entry-id` doesn't exist or isn't accessible by the user, or `comment-id` doesn't exist.
| 409         | One of the query parameters was passed more than once with different values. |

The response will be a JSON document with a `success` field indicating whether or not the request
succeeded. If `success` is `false`, a `reason` field will exist as well, providing a short, human
readable explanation of the failure.

#### 403

When a comment is being retracted, a 403 will result if neither the file or folder is owned by the
user nor was the comment created by the user.  When a comment is being readmitted, a 403 will result
if the comment wasn't originally retracted by the user.

### Example

```
curl -X PATCH "localhost/secured/filesystem/f86700ac-df88-11e3-bf3b-6abdce5a08d5/comments/79a6a1f0-0745-21e3-c125-73dece2a6989?proxyToken=fake-token&retracted=true"
```
```json
{ "success" : true }
```
