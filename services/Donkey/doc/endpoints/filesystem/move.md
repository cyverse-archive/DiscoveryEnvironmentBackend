Moving Files and/or Directories
--------------
__URL Path__: /secured/filesystem/move

This endpoint delegates to data-info's /mover endpoint.

__HTTP Method__: POST

__Error codes__: ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER

The ERR_DOES_NOT_EXIST error code pops up when the destination directory does not exist and when one of the "sources" directories does not exist.
The ERR_EXISTS code pops up when one of the new destination directories already exists.
The ERR_TOO_MANY_PATHS error code is returned when all of the "sources" and the items and sub-directories under them exceed the maximum number of paths that can be processed by this endpoint.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "sources" : [
            "/tempZone/home/rods/test1"
        ],
        "dest" : "/tempZone/home/rods/test"
    }

"sources" can contain a mix of files and directories.

__Response__:

    {
        "dest":"/tempZone/home/rods/test",
        "sources":[
            "/tempZone/home/rods/test1"
        ]
    }


__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"sources" : ["/tempZone/home/rods/test1"],"dest" :"/tempZone/home/rods/test"}' http://127.0.0.1:3000/secured/filesystem/move?proxyToken=notReal



Moving all items in a Directory
--------------

__URL Path__: /secured/filesystem/move-contents

This endpoint delegates to data-info's /data/:data-id/children/dir endpoint, after looking up the source's UUID.

__HTTP Method__: POST

__Error codes__: ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_TOO_MANY_PATHS, ERR_NOT_A_USER

The ERR_DOES_NOT_EXIST error code is returned when the "destination" directory does not exist and when the "source" directory does not exist.
The ERR_EXISTS error code is returned when one of the new destination directories or files already exists.
The ERR_TOO_MANY_PATHS error code is returned when all items in the "source" directory and its sub-directories exceed the maximum number of paths that can be processed by this endpoint.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "source": "/tempZone/home/rods/test1",
        "dest": "/tempZone/home/rods/test"
    }

__Response__:

    {
        "dest":"/tempZone/home/rods/test",
        "sources":[
            "/tempZone/home/rods/test1/test2",
            "/tempZone/home/rods/test1/test3"
        ]
    }

__Example ERR_TOO_MANY_PATHS Error Response__:

    {
        "error_code": "ERR_TOO_MANY_PATHS",
        "count": 250,
        "limit": 100
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"source": "/tempZone/home/rods/test1","dest": "/tempZone/home/rods/test"}' http://127.0.0.1:3000/secured/filesystem/move-contents?proxyToken=notReal

