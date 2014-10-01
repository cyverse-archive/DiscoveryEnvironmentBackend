Emptying a User's Trash Directory
---------------------------------
__URL Path__: /secured/filesystem/trash

__HTTP Method__: DELETE

__Error Codes__: ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Response__:

    {
        "action" : "delete-trash",
        "status" : "success",
        "trash" : "/path/to/user's/trash/dir/",
        "paths" : [
                "/path/to/deleted/file",
        ]
    }

__Curl Command__:

    curl -X DELETE http://127.0.0.1:3000/secured/filesystem/trash?proxyToken=notReal
