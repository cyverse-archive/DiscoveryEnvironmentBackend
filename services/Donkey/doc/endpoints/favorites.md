This document describes the favorites resource.

A _favorite_ is something that a user has decided is important enough that it should be more readily
accessible than other resources of the same time.

# Resources

## Favorite

A favorite is modeled as a UUID having the same value has the important resource's UUID.

**Example**

```
f86700ac-df88-11e3-bf3b-6abdce5a08d1
```

## Data Id Collection

A collection of data Ids is its own resource. It is a JSON document (media type `application/json`)
with a single `filesystem` field that holds an array of UUIDs, each for a file or folder.

**Example**

```json
{
  "filesystem" : [
    "f86700ac-df88-11e3-bf3b-6abdce5a08d1",
    "9cdb9492-f288-11e3-ab3e-13be1320bb50"
  ]
}
```

# Endpoints

## Marking a Data Resource as Favorite

`PUT /secured/favorites/filesystem/{favorite}`

This endpoint marks a given file or folder a favorite of the authenticated user. `{favorite}` is the
UUID of the file or folder being marked.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication.  Any additional parameters
will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The file or folder corresponding to the `<favorite>` UUID has been marked as a favorite of the authenticated user. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | The `{favorite}` UUID doesn't belong to a known file or folder or the file or folder isn't readable by the authenticated user. |

The response will be a JSON document with a `"success"` field indicating whether or not the request
succeeded. If `"success"` is `false`, a `"reason"` field will exist as well, providing a short,
human readable explanation of the failure.

### Example

```
? curl -XPUT localhost/secured/favorites/filesystem/f86700ac-df88-11e3-bf3b-6abdce5a08d1?proxyToken=fake-token
```
```json
{
  "success" : true
}
```

## Removing a Data Resource from Being a Favorite

`DELETE /secured/favorites/filesystem/{favorite}`

This endpoint removes a given file or folder from the authenticated user's favorites. `<favorite>`
is the UUID of the file or folder.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication. Any additional parameters
will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The file or folder corresponding to the `<favorite>` UUID has been marked as a favorite of the authenticated user. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | The file or folder corresponding to the `<favorite>` UUID wasn't marked as a favorite. |

The response will be a JSON document with a `"success"` field indicating whether or not the request
succeeded. If `"success"` is `false`, a `"reason"` field will exist as well, providing a short,
human readable explanation of the failure.

### Example

```
? curl -XDELETE localhost/secured/favorites/filesystem/f86700ac-df88-11e3-bf3b-6abdce5a08d1?proxyToken=fake-token
```
```json
{
  "success" : true
}
```

### Listing Stat Info for Favorite Data

`GET /secured/favorites/filesystem`

This endpoint lists stat information for the authenticated user's favorite files and folders. Only
files and folders accessible to the user will be listed.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication. Any additional parameters
will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The list of stat info was obtained and is included in the response body. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |

Upon success, the response body will be a [data collection](endpoints/filesystem/stat.md) JSON
document (but with the field be `filesystem` instead of `paths`) containing the stat information of
the favorite files and folders with an additional field `"success"` with the value `true`. The
format of the JSON maps is the same as that for the /secured/filesystem/stat endpoint.

Upon failure, a JSON document with `"success"` and `"reason"` fields will the returned. The
`"success"` field will have the value `false`.  The `"reason"` field will provide a short, human
readable explanation of the failure.

### Example

```
? curl localhost/secured/favorites/filesystem/favorites?proxyToken=fake-token
```
```json
{
    "filesystem": [
        {
            "date-created": 1.397233899e+12,
            "date-modified": 1.397233899e+12,
            "dir-count": 256,
            "file-count": 0,
            "id": "/iplant/home/wregglej/analyses",
            "label": "analyses",
            "path": "/iplant/home/wregglej/analyses",
            "permission": "own",
            "share-count": 1,
            "type": "dir",
            "uuid": "0d880c78-df8a-11e3-bfa5-6abdce5a08d5"
        }
    ],
    "success": true
}
```

## Filter a Set of Resources for Favorites

`POST /secured/favorites/filter`

This endpoint is used to indicate which resources in a provided set of resources are favorites of
the authenticated user. Only UUIDs for files and folders accessible to the user will be listed.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication. Any additional parameters
will be ignored.

The request body should be a [data id collection](#data-id-collection) JSON document containing the
UUIDs for the files and folders to be filtered.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The filtered list of UUIDs was generated and is included in the response body. |
| 400         | The request body wasn't a syntactically correct [data id collection](#data-id-collection). |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |

Upon success, the response body will be a [data id collection](#data-id-collection) JSON document
containing the UUIDs from the request body that correspond to favorite files and folders of the
user. There will be an additional field `"success"` with the value `true`.

Upon failure, a JSON document with `"success"` and `"reason"` fields will the returned. The
`"success"` field will have the value `false`.  The `"reason"` field will provide a short, human
readable explanation of the failure.

### Example

```
? curl -XPOST -d '
    {
      "filesystem" : [
        "f86700ac-df88-11e3-bf3b-6abdce5a08d1",
        "f86700ac-df88-11e3-bf3b-6abdce5a08d5"
      ]
    }' localhost/secured/favorites/filter?proxyToken=fake-token
```
```json
{
  "filesystem" : [
    "f86700ac-df88-11e3-bf3b-6abdce5a08d1"
  ],
  "success"    : true
}
```
