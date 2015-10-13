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

## Favorite Data Collection

A collection of favorite data or filesystem entities is its own resource. It is a JSON document
(media type `application/json`) with the following fields.

| Field   | Type   | Description |
| ------- | ------ | ----------- |
| files   | array  | an array of [favorite file objects](#favorite-file-object) |
| folders | array  | an array of [favorite folder objects](#favorite-folder-object) |
| total   | number | the total number of favorite files and folders for a given authenticated user |

### Favorite File Object

A favorite file object has the following fields.

| Field         | Type    | Description |
| ------------- | ------- | ----------- |
| date-created  | number  | the time when the file was created in ms since the POSIX epoch |
| date-modified | number  | the time when the file was last modified in ms since the POSIX epoch |
| file-size     | number  | the size in bytes of the file |
| id            | string  | the absolute path to the file |
| info-type     | string  | the semantic type of the content of the file |
| isFavorite    | boolean | `true` |
| label         | string  | the name of file relative to its containing folder |
| mime-type     | string  | the media type of the file |
| path          | string  | the absolute path to the file |
| permission    | string  | the permission the authenticated user has on the file, `"read"|"write"|"own"` |
| share-count   | number  | the number of other users this file has been shared with |
| type          | string  | `"file"` |
| uuid          | string  | the file's UUID |

### Favorite Folder Object

A favorite folder object has the following fields.

| Field         | Type    | Description |
| ------------- | ------- | ----------- |
| date-created  | number  | the time when the folder was created in ms since the POSIX epoch |
| date-modified | number  | the time when the folder was last modified (i.e. had a member file or folder added or removed) in ms since the POSIX epoch |
| dir-count     | number  | the number of member folders |
| file-count    | number  | the number of member files |
| file-size     | number  | `0` |
| id            | string  | the absolute path to the folder |
| isFavorite    | boolean | `true` |
| label         | string  | the name of folder relative to its containing folder |
| path          | string  | the absolute path to the folder |
| permission    | string  | the permission the authenticated user has on the folder, `"read"|"write"|"own"` |
| share-count   | number  | the number of other users this folder has been shared with |
| type          | string  | `"dir"` |
| uuid          | string  | the folder's UUID |

# Endpoints

## Marking a Data Resource as Favorite

`PUT /secured/favorites/filesystem/{favorite}`

Delegates to metadata: `PUT /favorites/filesystem/{favorite}`

This endpoint marks a given file or folder a favorite of the authenticated user. `{favorite}` is the
UUID of the file or folder being marked.

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

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

Error responses may contain a `"reason"` field, providing a short, human readable explanation of the failure.

### Example

```
? curl -XPUT localhost/secured/favorites/filesystem/f86700ac-df88-11e3-bf3b-6abdce5a08d1?proxyToken=fake-token
```

## Removing a Data Resource from Being a Favorite

`DELETE /secured/favorites/filesystem/{favorite}`

Delegates to metadata: `DELETE /favorites/filesystem/{favorite}`

This endpoint removes a given file or folder from the authenticated user's favorites. `<favorite>`
is the UUID of the file or folder.

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication. Any additional parameters
will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The file or folder corresponding to the `favorite` UUID has been marked as a favorite of the authenticated user. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |
| 404         | The file or folder corresponding to the `favorite` UUID wasn't marked as a favorite. |

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl -XDELETE localhost/secured/favorites/filesystem/f86700ac-df88-11e3-bf3b-6abdce5a08d1?proxyToken=fake-token
```

### Listing Stat Info for Favorite Data

`GET /secured/favorites/filesystem`

Requires metadata endpoint: `GET /favorites/filesystem`

This endpoint lists stat information for the authenticated user's favorite files and folders. Only
files and folders accessible to the user will be listed. The result set is paged.

This endpoint fetches favorite information from the corresponding metadata service endpoint before
applying filters, sorting, and additional stat information.
Please see the metadata documentation for more information.

### Request

A request to this endpoint requires the parameters in the following table.

| Parameter   | Required? | Default      | Description |
| ----------- | --------- | ------------ | ----------- |
| proxyToken  | yes       |              | the CAS authentication token |
| sort-col    | yes       |              | the field used to sort the filesystem entries in the result set. This can be `NAME|ID|LASTMODIFIED|DATECREATED|SIZE`. All values are case insensitive. |
| sort-dir    | yes       |              | the sorting direction. It can be `ASC|DESC`. Both values are case insensitive. |
| limit       | yes       |              | the maximum number of filesystem entries to return |
| offset      | yes       |              | the number entries in the sorted total result set to skip before including entries in the response document. |
| entity-type | no        | ANY          | the type of entities to return. It can be `ANY|FILE|FOLDER`. All values are case-insensitive. |
| info-type   | no        | no filtering | filter the files portion of the result set so that only files with this info type are returned. To return multiple info types, and this parameter more than once. |

Any additional parameters will be ignored.

Any body attached to the request will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The list of stat info was obtained and is included in the response body. |
| 400         | one of the parameters was missing or had a nonsensical value. |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |

Upon success, the response body will be a [data collection](#favorite-data-collection) JSON document
containing the stat information of the favorite files and folders.

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

### Example

```
? curl "localhost/secured/favorites/filesystem/favorites?proxyToken=fake-token&sort-col=ID&sort-dir=ASC&limit=1&offset=0"
```
```json
{
    "filesystem": {
        "files": [],
        "folders": [
            {
                "date-created": 1.397233899e+12,
                "date-modified": 1.397233899e+12,
                "dir-count": 256,
                "file-count": 0,
                "id": "/iplant/home/wregglej/analyses",
                "isFavorite": true,
                "label": "analyses",
                "path": "/iplant/home/wregglej/analyses",
                "permission": "own",
                "share-count": 1,
                "type": "dir",
                "uuid": "0d880c78-df8a-11e3-bfa5-6abdce5a08d5"
            }
        ],
        "total": 3
    }
}
```

## Filter a Set of Resources for Favorites

`POST /secured/favorites/filter`

Delegates to metadata: `POST /favorites/filter`

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

This endpoint fetches the filtered favorite IDs from the corresponding metadata service endpoint
before further filtering only UUIDs for files and folders accessible to the user.

### Request

A request to this endpoint requires no parameters beyond the `proxyToken` authentication parameter.
The user that owns the favorite is determined from the authentication. Any additional parameters
will be ignored.

### Response

| Status Code | Cause |
| ----------- | ----- |
| 200         | The filtered list of UUIDs was generated and is included in the response body. |
| 400         | The request body wasn't a syntactically correct [data id collection](#data-id-collection). |
| 401         | Either the `proxyToken` was not provided, or the value wasn't correct. |

Upon success, the response body will be a [data id collection](#data-id-collection) JSON document
containing the UUIDs from the request body that correspond to favorite files and folders of the
user.

Error responses may include a `reason` field, providing a short, human readable explanation of the failure.

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
  ]
}
```
