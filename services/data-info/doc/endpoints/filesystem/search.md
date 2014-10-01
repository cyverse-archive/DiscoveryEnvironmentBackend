This document describes the endpoints used to performing searches of user data.

# Table of Contents

* [Indexed Information](#indexed-information)
* [Basic Usage](#basic-usage)
    * [Search Requests](#search-requests)
    * [Index Status Request](#index-status-request)
* [Administration](#administration)
    * [Search Requests by Proxy](#search-requests-by-proxy)
    * [Update Index Request](#update-index-request)

# Indexed Information

For each file and folder stored in the iPlant data store, its ACL, system metadata, and user
metadata are indexed as a JSON document. For files, [file records](../../schema.md#file-record) are
indexed, and for folders, [folder records](../../schema.md#folder-record) are indexed.

# Basic Usage

For the client without administrative privileges, data-info provides endpoints for performing searches
and for checking the status of the indexer.

## Search Requests

data-info provides search endpoints that allow callers to search the data by name and various pieces of
system and user metadata. It supports the all the queries in the [ElasticSearch query DSL]
(http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-queries.html) for
searching.

Each field in an indexed document may be explicitly used in a search query. If the field is an
object, i.e. an aggregate of fields, the object's fields may be explicitly referenced as well using
dot notation, e.g. `acl.access`.

### Endpoints

* `GET /secured/filesystem/index`

This endpoint searches the data index and retrieves a set of files and/or folders matching the terms
provided in the query string.

### Request Parameters

The following additional URI parameters are recognized.

| Parameter | Required? | Default    | Description |
| --------- | --------- | ---------- | ----------- |
| q         | yes*      |            | This parameter holds a JSON encoded search query. See [query syntax](http://www.elasticsearch.org/guide/en/elasticsearch/reference/current/query-dsl-queries.html) for a description of the syntax. |
| type      | no        | any        | This parameter restricts the search to either files or folders. It can take the values `any`, meaning files and folders, `file`, only files, and `folders`, only folders. |
| tags      | yes*      |            | This is a comma-separated list of tag UUIDs. This parameter restricts the search to only entities that have at least of the provided tags. |
| offset    | no        | 0          | This parameter indicates the number of matches to skip before including any in the result set. When combined with `limit`, it allows for paging results. |
| limit     | no        | 200        | This parameter limits the number of matches in the result set to be a most a certain amount. When combined with `offset`, it allows for paging results. |
| sort      | no        | score:desc | See [sorting](#sorting) |

\* At least `q` or `tags` is required.

#### Sorting

The result set is sorted. By default the sort is performed on the `score` field in descending order.
The `sort` request parameter can be used to change the sort order.  Its value has the form
`field:direction` where `field` is one of the fields of a **match record** and `direction` is either
`asc` for ascending or `desc` for descending.  Use dot notation to sort by one of the nested fields
of the match records, e.g., `entity.label` will sort by the `label` field of the matched entities.
If no `:direction` is provided, the search direction will default to `desc`.

### Response

When the search succeeds the response document has these additional fields.

| Field          | Type   | Description |
| -------------- | ------ | ----------- |
| total          | number | This is the total number of matches found, not the number of elements in the `matches` array. |
| offset         | number | This is the value of the `offset` parameter in the query string. |
| matches        | array  | This is the set or partial set of matches found, each entry being a **match record**. It contains at most `limit` entries and is sorted by descending score. |
| execution-time | number | This is the number of milliseconds that it took to perform the query and get a response from elasticsearch. |

**Match Record**

| Field  | Type   | Description |
| ------ | ------ | ----------- |
| score  | number | an indication of how well this entity matches the query compared to other matches |
| type   | string | the entity is this type of filesystem entry, either `"file"` or `"folder"` |
| entity | object | the [file record](../../schema.md#file-record) or [folder record](../../schema.md#folder-record) matched |

### Example

```
$ curl \
> "http://localhost:8888/secured/filesystem/index?proxyToken=$(cas-ticket)&q=\\{\"wildcard\":\\{\"label\":\"?e*\"\\}\\}&type=file&tags=64581dbe-3aa1-11e4-a54e-3c4a92e4a804,6504ca14-3aa1-11e4-8d83-3c4a92e4a804&offset=1&limit=2&sort:desc" \
> | python -mjson.tool
{
    "matches": [
        {
            "entity": {
                "creator": "rods#iplant",
                "dateCreated": 1381325224090,
                "dateModified": 1384998366001,
                "fileSize": 13225,
                "id": "6278f8e0-121f-4ce6-a15f-1083cfad6de5",
                "label": "read1_10k.fq",
                "fileType": null,
                "metadata": [],
                "path": "/iplant/home/rods/analyses/fc_01300857-2013-10-09-13-27-04.090/read1_10k.fq",
                "permission" : "own",
                "userPermissions": [
                    {
                        "permission": "read",
                        "user": "rods#iplant"
                    },
                    {
                        "permission": "own",
                        "user": "rodsadmin#iplant"
                    }
                ]
            },
            "score": 1.0,
            "type": "file"
        },
        {
            "entity": {
                "creator": "rods#iplant",
                "dateCreated": 1381325285602,
                "dateModified": 1384998366001,
                "fileSize": 14016,
                "fileType": null,
                "id": "acaeb63d-8e84-412d-89a2-716a6a4dda7e",
                "label": "read1_10k.fq",
                "metadata": [
                    {
                        "attribute": "color",
                        "unit": null,
                        "value": "red"
                    }
                ],
                "path": "/iplant/home/rods/analyses/ft_01251621-2013-10-09-13-28-05.602/read1_10k.fq",
                "permission": "write",
                "userPermissions": [
                    {
                        "permission": "read",
                        "user": "rods#iplant"
                    },
                    {
                        "permission": "write",
                        "user": "rodsadmin#iplant"
                    }
                ]
            },
            "score": 1.0,
            "type": "file"
        }
    ],
    "execution-time" : 300,
    "offset": 1,
    "success": true,
    "total": 7
}
```

## Index Status Request

__NOT IMPLEMETED YET__

A client may request the status of the indexer.

### Endpoint

* Endpoint: `GET /secured/filesystem/index-status`

### Request Parameters

There are no additional request parameters.

### Response

| Field               | Type   | Description |
| ------------------- | ------ | ----------- |
| lag                 | number | This is the estimated number of seconds the index state lags the data store state. |
| size                | number | This is the number of entries in the index. |
| lastSyncTime        | string | This is the time when the last successful synchronization was requested in ISO 8601 format. |
| syncState           | string | This is the current synchronizer state of the index. It must be `"idle"`, `"indexing"` or `"pruning"`.\* |
| syncProcessingCount | number | This is the total number of elements to consider for indexing or pruning during the current synchronizer state. |
| syncProcessedCount  | number | This is the total number of elements that have be indexed or pruned during the current synchronizer state. |

\* Unless a synchronization has been requested, the `syncState` will be `"idle"`. When a
synchronization has been requested, `syncState` is transitioned to `"indexing`". This means that the
data store is being crawled and entries missing from the search index are being added. When indexing
has been completed, `syncState` is transitioned to `"pruning"`. This means that the search index is
scanned and entries that are no longer in the data store are removed. Finally, when pruning has
completed, `syncState` is transitioned back to `"idle"`.

### Example

```
$ curl http://localhost:8888/secured/filesystem/index/status?proxyToken=$(cas-ticket) \
> | python -mjson.tool
{
    "lag": 11,
    "lastSyncTime": "2013-11-21T19:54:00.000Z",
    "size": 110432665,
    "success": true,
    "syncProcessedCount": 0,
    "syncProcessingCount": 0,
    "syncState": "idle"
}
```

# Administration

__NOT IMPLEMENTED YET__

For clients with administrative privileges, data-info provides additional endpoints for performing
search requests as a specific user and controlling the indexer.

## Search Requests by Proxy

An administrator can perform any search as a specific user.

### Endpoints:

* Admin Endpoint: `GET /admin/filesystem/index`

This endpoint searches the data index and retrieves a set of files and/or folders matching the terms
provided in the query string.

### Request Parameters

The request is encoded as a JSON query. It supports all of the parameters of a
[normal search request](#search-request) with one additional parameter. The  `as-user` parameter
identifies the user the administrator is performing the search as. This allows the administrator to
reproduce a query the user has complained about. The parameter value takes the form
`{username}#{zone}` where `username` is the user identity and `zone` is the authentication zone.

### Response

The response body is the same as a [normal response body](#response-body).

### Example

```
$ curl \
> "http://localhost:8888/admin/filesystem/search/iplant/home?proxyToken=$(cas-ticket)&as-user=rods#iplant&q=\\{\"wildcard\":\\{\"label\":\"?e*\"\\}\\}&type=file&offset=1&limit=2&sort=score:desc" \
> | python -mjson.tool
{
    "matches": [
        {
            "entity": {
                "creator": "rods#iplant",
                "dateCreated": 1381325224090,
                "dateModified": 1381325224090,
                "fileSize": 13225,
                "fileType": null,
                "id": "6278f8e0-121f-4ce6-a15f-1083cfad6de5",
                "label": "read1_10k.fq",
                "metadata": [],
                "path": "/iplant/home/rods/analyses/fc_01300857-2013-10-09-13-27-04.090/read1_10k.fq",
                "permission": "own",
                "userPermissions": [
                    {
                        "permission": "read",
                        "user": "rods#iplant"
                    },
                    {
                        "permission": "own",
                        "user": "rodsadmin#iplant"
                    }
                ]
            },
            "score": 1.0,
            "type": "file"
        },
        {
            "entity": {
                "creator": "rods#"iplant",
                "dateCreated": 1381325285000,
                "dateModified": 1381325285000,
                "fileSize": 14016,
                "fileType": null,
                "id": "acaeb63d-8e84-412d-89a2-716a6a4dda7e",
                "label": "read1_10k.fq",
                "metadata": [
                    {
                        "attribute": "color",
                        "unit": null,
                        "value": "red"
                    }
                ],
                "path": "/iplant/home/rods/analyses/ft_01251621-2013-10-09-13-28-05.602/read1_10k.fq",
                "permission": "write",
                "userPermissions": [
                    {
                        "permission": "read",
                        "user": "rods#iplant"
                    },
                    {
                        "permission": "write",
                        "user": "rodsadmin#iplant"
                    }
                ]
            },
            "score": 1.0,
            "type": "file"
        }
    ],
    "offset": 1,
    "success": true,
    "total": 7
}
```


## Update Index Request

data-info provides an endpoint for updating the index used by search.

### Endpoint

* Endpoint: `POST /admin/filesystem/index`

### Request Parameter

An indexing request has one additional parameter. The `sync` parameter indicates what operation the
synchronizer should perform on the index. If the parameter is set to `start-full`, a full
synchronization of the index with the data store will be performed. If the parameter is set to
`start-incremental`, only the items in the data store that have been created or modified since the
last completed synchronization will be indexed during this synchronization. Finally, if the
parameter is set to `stop`, if a synchronization is currently in progress, it will be terminated.

### Response

A successful response has no additional fields.

### Example

```
$ curl -X POST http://localhost:8888/admin/filesystem/index?proxyToken=$(cas-ticket) \
> | python -mjson.tool
{
    "success": true
}
```

