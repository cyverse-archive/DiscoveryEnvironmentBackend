Directory Creation
------------------
__URL Path__: /secured/filesystem/directory/create

__HTTP Method__: POST

__Error Codes__: ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_NOT_A_USER

__Request Body__:

```json
{"path" : "/tempZone/home/rods/test3"}
```

__Response Body__:

This endpoint uses a similar response as the [/secured/filesystem/stat](stat.md#file-and-directory-status-information) endpoint.
For example:

```json
{
    "success": true,
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
