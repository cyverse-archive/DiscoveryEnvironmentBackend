This document describes the tags resource.

A _tag_ is a user-defined label that can be attached to files and folders to relate them to each
other.

# Resources

## Tag

A tag is modeled as a JSON document (media type `application/json`) with the fields described in the
following table.

| Field       | Type   | Description |
| ----------- | ------ | ----------- |
| id          | string | the service-provided UUID associated with the tag |
| value       | string | the value used to identify the tag, at most 255 characters in length |
| description | string | a description of the purpose of the tag |

# Endpoints

## Creating a tag

`POST /secured/tags/user`

This endpoint creates a tag for use by the authenticated user.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the tag is determined from the authentication. Any additional parameters will be
ignored.

The request body should be a JSON document (media type `application/json`) with the fields
described in the following table.

| Field       | Type   | Description |
| ----------- | ------ | ----------- |
| value       | string | the value used to identify the tag, at most 255 characters in length |
| description | string | (optional) a description of the purpose of the tag |

These are just the `value` and `description` fields of the tag being created.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 201         | The tag was successfully created |
| 400         | The request body wasn't syntactically correct |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 413         | The `value` was too long or the request body was too large. |

Upon success, the response body will be a JSON document with `"id"` and `"success"` fields. The
`"id"` field will contain the UUID the service assigned to the newly created tag. The `"success"`
field will have the value `true`.

Upon failure, a JSON document with `"success"` and `"reason"` fields will the returned. The
`"success"` field will have the value `false`.  The `"reason"` field will provide a short, human
readable explanation of the failure.

### Example

```
? curl -XPOST -d '{ "value" : "a tag" }' localhost/secured/tags/user?proxyToken=fake-token
```
```json
{
  "id"      : "f86700ac-df88-11e3-bf3b-6abdce5a08d1",
  "success" : true
}
```

## Update a tag's label and/or description

`PATCH /secured/tags/user/{tag-id}`

This endpoint allows a tag's label and description to be modified by the owning user.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the tag is determined from the authentication. Any additional parameters will be
ignored.

The request body should be a JSON document (media type `application/json`) with the fields
described in the following table.

| Field       | Type   | Description |
| ----------- | ------ | ----------- |
| value       | string | (optional) a new value for the tag at most 255 characters in length |
| description | string | (optional) a new description of the purpose of the tag |

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tag was successfully updated |
| 400         | The request body wasn't syntactically correct |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 413         | The `value` was too long or the request body was too large. |

The response will be a JSON document with a `"success"` field indicating whether or not the request
succeeded. If `"success"` is `false`, a `"reason"` field will exist as well, providing a short,
human readable explanation of the failure.

### Example

```
? curl -XPATCH -d '{ "description" : "an example tag" }' localhost/secured/tags/user/f86700ac-df88-11e3-bf3b-6abdce5a08d1?proxyToken=fake-token
```
```json
{
  "success" : true
}
```

## Delete a tag

`DELETE /secured/tags/user/{tag-id}`

This endpoint allows a user tag to be deleted, detaching it from all metadata.  `tag-id` is the UUID
of the tag to delete.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the tag is determined from the authentication. Any additional parameters will be
ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tag was successfully deleted |
| 404         | `tag-id` wasn't a UUID of a tag owned by the authenticated user |

The response will be a JSON document with a `"success"` field indicating whether or not the request
succeeded. If `"success"` is `false`, a `"reason"` field will exist as well, providing a short,
human readable explanation of the failure.

### Example

```
? curl -X DELETE localhost/secured/tags/user/7cd71660-fe1a-11e3-89ea-23963e1ca21b?proxyToken=fake-token
```
```json
{ "success" : true }
```

## Suggest a tag

`GET /secured/tags/suggestions`

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

Upon success, the response body will be a JSON document with `"tags"` and `"success"` fields. The
`"tags"` field will contain an array of the [tag](#tag) objects whose values contain the fragment
from the request. The `"success"` field will have the value `true`.

Upon failure, a JSON document with `"success"` and `"reason"` fields will the returned. The
`"success"` field will have the value `false`.  The `"reason"` field will provide a short, human
readable explanation of the failure.

### Example

```
curl "localhost/secured/tags/suggestions?proxyToken=fake&contains=a"
```
```json
{
  "success" : true,
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

Depending on the `type` parameter, this endpoint either attaches a set of the authenticated user's
tags to the indicated file or folder, or it detaches the set. `{entry-id}` is the UUID associated
with the file or folder receiving the tags.

### Request

Other than the `proxyToken` authentication parameter, this endpoint requires a `type` parameter. If
the `type` parameter's value is `attach`, the provided set of tags will be attached to the file or
folder. If the value is `detach`, the set will be detached.

Any additional parameters will be ignored.

The request body needs to be a JSON document (media type `application/json`) containing a single
field `tags` contain and array of UUIDs.  These are the UUIDs of the tags to be attached or
detached.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The tags were attached or detached from the file or folder |
| 400         | The `type` wasn't provided or had a value other than `attach` or `detach`; or the request body wasn't syntactically correct |
| 403         | One of the provided tag Ids doesn't map to a tag for the authenticated user. |
| 404         | The `{entry-id}` UUID doesn't belong to a known file or folder or the file or folder isn't readable by the authenticated user. |


The response will be a JSON document with a `"success"` field indicating whether or not the request
succeeded. If `"success"` is `false`, a `"reason"` field will exist as well, providing a short,
human readable explanation of the failure.

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
```json
{
  "success" : true
}
```

## Listing attached tags

`GET /secured/filesystem/entry/{entry-id}/tags`

This endpoint lists the tags of the calling authenticated user that are attached to the indicated
file or folder. `{entry-id}` is the UUID of the file or folder the caller is inspecting. The file or
folder must be readable by the authenticated user.

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

Upon success, the response body will be a JSON document (media type `application/json`) that
contains a `tags` field holding an array of [tag](#tag) objects. These are the tags that are
attached to the file or folder with id `{entry-id}`. There will be an additional field `success`
with the value `true`.

Upon failure, a JSON document with `success` and `reason` fields will the returned. The `success`
field will have the value `false`.  The `reason` field will provide a short, human readable
explanation of the failure.

### Example

```
? curl localhost/secured/filesystem/entry/f86700ac-df88-11e3-bf3b-6abdce5a08d5/tags?proxyToken=fake-token
```
```json
{
  "success" : true,
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
