Deleting Files and/or Directories
---------------------------------
__URL Path__: /secured/filesystem/delete

__HTTP Method__: POST

__Action__: "delete"

__Error Codes__: ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER

The ERR_TOO_MANY_PATHS error code is returned when all of the "paths" and the items and sub-directories under them exceed the maximum number of paths that can be processed by this endpoint.

__Request Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : ["/tempZone/home/rods/test2"]
    }

"paths" can take a mix of files and directories.

__Response__:

    {
        "action":"delete-dirs",
        "paths":["/tempZone/home/rods/test2"]
        "status" : "success"
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"paths" : ["/tempZone/home/rods/test2"]}' http://127.0.0.1:3000/secured/filesystem/delete?user=rods


Deleting all items in a Directory
--------------

__URL Path__: /secured/filesystem/delete-contents

__HTTP Method__: POST

__Action__: "delete"

__Error codes__: ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER

The ERR_TOO_MANY_PATHS error code is returned when all items in the source directory and its sub-directories exceed the maximum number of paths that can be processed by this endpoint.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "path": "/tempZone/home/rods/test"
    }

__Response__:

    {
        "paths":[
            "/tempZone/home/rods/test/test1",
            "/tempZone/home/rods/test/test2"
        ],
        "success" : true
    }

__Example ERR_TOO_MANY_PATHS Error Response__:

    {
        "success": false,
        "error_code": "ERR_TOO_MANY_PATHS",
        "count": 250,
        "limit": 100
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"path": "/tempZone/home/rods/test"}' http://127.0.0.1:3000/secured/filesystem/delete-contents?proxyToken=notReal
