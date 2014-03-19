Renaming a File or Directory
----------------------------
__URL Path__: /secured/filesystem/rename

__HTTP Method__: POST

__Error codes__: ERR_NOT_A_FOLDER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_INCOMPLETE_RENAME, ERR_NOT_A_USER, ERR_TOO_MANY_PATHS

The ERR_TOO_MANY_PATHS error code is returned when the items and sub-directories under the "source" folder exceed the maximum number of paths that can be processed by this endpoint.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "source" : "/tempZone/home/wregglej/test3",
        "dest" : "/tempZone/home/wregglej/test2"
    }

__Response__:

    {
        "source":"/tempZone/home/wregglej/test3",
        "dest":"/tempZone/home/wregglej/test2",
        "status":"success"
    }


__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"source" : "/tempZone/home/wregglej/test3", "dest" : "/tempZone/home/wregglej/test2"}' http://127.0.0.1:3000/secured/filesystem/rename?proxyToken=notReal




