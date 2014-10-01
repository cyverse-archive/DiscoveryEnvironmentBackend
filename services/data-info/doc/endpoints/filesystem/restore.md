Restoring a file or directory from a user's trash
-------------------------------------------------

__URL Path__: /secured/filesystem/restore

__HTTP Method__: POST

__Error Codes__: ERR_EXISTS, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS

The ERR_TOO_MANY_PATHS error code is returned when all of the "paths" and the items and sub-directories under them exceed the maximum number of paths that can be processed by this endpoint.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : ["/iplant/trash/home/proxy-user/johnworth/foo.fq",
                   "/iplant/trash/home/proxy-user/johnworth/foo1.fq"]
    }

__Response Body__:

    {
        "status" : "success",
        "restored" : {
            "/iplant/trash/home/proxy-user/johnworth/foo.fq" :  {
                "restored-path" : /iplant/home/johnworth/foo.fq",
                "partial-restore" : true
            },
            "/iplant/trash/home/proxy-user/johnworth/foo1.fq" : {
                "restored-path" : "/iplant/home/johnworth/foo1.fq"
                "partial-restore" : true
            }
        }
    }

The "restored" field is a map that whose keys are the paths in the trash that were restored. Associated with those paths is a map that contains two entries, "restored-path" that contains the path that the file was restored to, and "partial-restore" which is a boolean that is true if the restoration was to the home directory because there was no alternative and false if the restoration was a full restore.

If a file is deleted and then the directory that the file originally lived in is deleted, the directory will be recreated if the file is restored. If the deleted directory is then subsequently restored, it will moved back to its original location with a numerical suffix. The suffix is generated according to how many directories with the same name exist in the same parent directory. File restorations follow similar logic. If a file with the same name is restored to the same directory multiple times, the subsequent restored versions will have numerical suffixes as well.

__Curl Command__:

    curl -d '{"paths" : ["/iplant/trash/home/proxy-user/johnworth/foo.fq", "/iplant/trash/home/proxy-user/johnworth/foo1.fq"]}' http://sample.nibblonian.org/secured/filesystem/restore?proxyToken=notReal


Restoring all items in a user's trash
--------------

__URL Path__: /secured/filesystem/restore-all

__HTTP Method__: POST

__Error Codes__: ERR_EXISTS, ERR_NOT_A_USER, ERR_NOT_WRITEABLE, ERR_TOO_MANY_PATHS

The ERR_TOO_MANY_PATHS error code is returned when all items in the user's trash and its sub-directories exceed the maximum number of paths that can be processed by this endpoint.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    No body is required for this request.

__Response__:

    {
        "success" : true,
        "restored" : {
            "/iplant/trash/home/proxy-user/johnworth/foo.fq" :  {
                "restored-path" : /iplant/home/johnworth/foo.fq",
                "partial-restore" : true
            },
            "/iplant/trash/home/proxy-user/johnworth/foo1.fq" : {
                "restored-path" : "/iplant/home/johnworth/foo1.fq"
                "partial-restore" : true
            }
        }
    }

__Example ERR_TOO_MANY_PATHS Error Response__:

    {
        "success": false,
        "error_code": "ERR_TOO_MANY_PATHS",
        "count": 250,
        "limit": 100
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -X POST http://127.0.0.1:3000/secured/filesystem/restore-all?proxyToken=notReal
