Directory Creation
------------------
Creates a directory, as well as any intermediate directories that do not already exist, given as a
path in the request. For example, if the path `/tempZone/home/rods/test1/test2/test3` is given in
the request, the `/tempZone/home/rods/test1` directory does not exist, and the requesting user has
write permissions on the `/tempZone/home/rods` folder, then all 3 `test*` folders will be created
for the requesting user.

__URL Path__: /secured/filesystem/directory/create

__HTTP Method__: POST

__Error Codes__: ERR_BAD_OR_MISSING_FIELD, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Request Body__:

```json
{"path" : "/tempZone/home/rods/test1/test2/test3"}
```

__Response Body__:

This endpoint uses a similar response as the [/stat-gatherer](stat-gatherer.md#file-and-folder-status-information) endpoint.
For example:

```json
{
    "success": true,
    "id": "a1212364-e3c3-11e4-8920-6abdce5a08d5",
    "path": "/tempZone/home/rods/test1/test2/test3",
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

    curl -H "Content-Type:application/json" -d '{"path" : "/tempZone/home/rods/test1/test2/test3"}' "http://127.0.0.1:3000/data/directory/create?proxyToken=notReal"
