Batch Directory Creation
------------------------

Secured Endpoint: POST /secured/filesystem/directories

Delegates to data-info: POST /data/directories

This endpoint is a passthrough to the data-info endpoint above.
Please see the data-info documentation for more information.

Directory Creation
------------------
__URL Path__: /secured/filesystem/directory/create

__HTTP Method__: POST

__Error Codes__: ERR_BAD_OR_MISSING_FIELD, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Request Body__:

```json
{"path" : "/tempZone/home/rods/test3"}
```

__Response Body__:

This endpoint uses a similar response as the [/secured/filesystem/stat](stat.md#file-and-directory-status-information) endpoint.
For example:

```json
{
    "id": "/tempZone/home/rods/test3",
    "path": "/tempZone/home/rods/test3",
    "label": "test3",
    "type": "dir",
    "date-modified": 1397063483000,
    "date-created": 1397063483000,
    "permission": "own",
    "share-count": 0,
    "dir-count": 0,
    "file-count": 0
}
```

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"path" : "/tempZone/home/rods/test3"}' "http://127.0.0.1:3000/secured/filesystem/directory/create?proxyToken=notReal"
