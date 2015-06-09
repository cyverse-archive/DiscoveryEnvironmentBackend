This document describes the tags resource.

A _tag_ is a user-defined label that can be attached to files and folders to relate them to each
other.

# Endpoints

## Creating a tag

`POST /secured/tags/user`

Delegates to metadata: `POST /tags/user`

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the tag is determined from the authentication. Any additional parameters will be
ignored.

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tag was successfully created |
| 400         | The request body wasn't syntactically correct |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 413         | The `value` was too long or the request body was too large. |

Upon success, the response body will be a JSON document with an `"id"` field. The
`"id"` field will contain the UUID the service assigned to the newly created tag.

Upon failure, a JSON document with a `"reason"` field will the returned. The `"reason"` field will
provide a short, human readable explanation of the failure.

### Example

```
? curl -XPOST -d '{ "value" : "a tag" }' localhost/secured/tags/user?proxyToken=fake-token
```
```json
{
  "id"      : "f86700ac-df88-11e3-bf3b-6abdce5a08d1"
}
```

## Update a tag's label and/or description

`PATCH /secured/tags/user/{tag-id}`

Delegates to metadata: `PATCH /tags/user/{tag-id}`

This endpoint allows a tag's label and description to be modified by the owning user.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the tag is determined from the authentication. Any additional parameters will be
ignored.

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tag was successfully updated |
| 400         | The request body wasn't syntactically correct |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 413         | The `value` was too long or the request body was too large. |

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl -XPATCH -d '{ "description" : "an example tag" }' localhost/secured/tags/user/f86700ac-df88-11e3-bf3b-6abdce5a08d1?proxyToken=fake-token
```

## Delete a tag

`DELETE /secured/tags/user/{tag-id}`

Delegates to metadata: `DELETE /tags/user/{tag-id}`

This endpoint allows a user tag to be deleted, detaching it from all metadata.

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the tag is determined from the authentication. Any additional parameters will be
ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tag was successfully deleted |
| 404         | `tag-id` wasn't a UUID of a tag owned by the authenticated user |

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl -X DELETE localhost/secured/tags/user/7cd71660-fe1a-11e3-89ea-23963e1ca21b?proxyToken=fake-token
```

## Suggest a tag

`GET /secured/tags/suggestions`

Delegates to metadata: `GET /tags/suggestions`

Given a textual fragment of a tag's value, this endpoint will list up to a given number of the
authenticated user's tags that contain the fragment.

### Request

A request to this endpoint requires the parameters in the following table.

| Parameter  | Description |
| ---------- | ----------- |
| proxyToken | the CAS authentication token |
| contains   | the value fragment |
| limit      | (optional) the maximum number of suggestions to return. no limit means return all |

Any additional parameters will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | zero or more suggestions were returned |
| 400         | the `contains` parameter was missing or the `limit` parameter was set to a something other than a non-negative number. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
curl "localhost/secured/tags/suggestions?proxyToken=fake&contains=a"
```
```json
{
  "tags"    : [
    {
      "description" : "",
      "id"          : "f86700ac-df88-11e3-bf3b-6abdce5a08d1",
      "value"       : "a tag"
    },
    {
      "description" : "creepy files that should never have been created",
      "id"          : "0e7a35ac-df8a-11e3-bfa5-6abdce5a08d5",
      "value"       : "Frankenstein"
    }
  ]
}
```

## Attaching or detaching multiple tags to a file or folder

`PATCH /secured/filesystem/entry/{entry-id}/tags`

Delegates to metadata: `PATCH /filesystem/entry/{entry-id}/tags`

### Request

Other than the `proxyToken` authentication parameter, this endpoint requires a `type` parameter.
Any additional parameters will be ignored.

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tags were attached or detached from the file or folder |
| 400         | The `type` wasn't provided or had a value other than `attach` or `detach`; or the request body wasn't syntactically correct |
| 403         | One of the provided tag Ids doesn't map to a tag for the authenticated user. |
| 404         | The `{entry-id}` UUID doesn't belong to a known file or folder or the file or folder isn't readable by the authenticated user. |


Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl -XPATCH -d '
    {
      "tags" : [
        "f86700ac-df88-11e3-bf3b-6abdce5a08d1",
        "0e7a35ac-df8a-11e3-bfa5-6abdce5a08d5"
      ]
    }' "localhost/secured/filesystem/entry/f86700ac-df88-11e3-bf3b-6abdce5a08d5/tags?proxyToken=fake-token&type=attach"
```

## Listing attached tags

`GET /secured/filesystem/entry/{entry-id}/tags`

Delegates to metadata: `GET /filesystem/entry/{entry-id}/tags`

This endpoint lists the tags of the calling authenticated user that are attached to the indicated
file or folder. The file or folder must be readable by the authenticated user.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication.  Any additional parameters
will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tags are listed in the response |
| 404         | The `{entry-id}` UUID doesn't belong to a known file or folder or the file or folder isn't readable by the authenticated user. |

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl localhost/secured/filesystem/entry/f86700ac-df88-11e3-bf3b-6abdce5a08d5/tags?proxyToken=fake-token
```
```json
{
  "tags"    : [
    {
      "description" : "",
      "id"          : "f86700ac-df88-11e3-bf3b-6abdce5a08d1",
      "value"       : "a tag"
    },
    {
      "description" : "creepy files that should never have been created",
      "id"          : "0e7a35ac-df8a-11e3-bfa5-6abdce5a08d5",
      "value"       : "Frankenstein"
    }
  ]
}
```
