Getting the path to a user's trash directory
--------------------------------------------
__URL Path__: /secured/filesystem/user-trash-dir

__HTTP Method__: GET

__Error codes__: ERR_NOT_A_USER

__Request Query Parameters__:
* proxyToken - A valid CAS ticket.

__Response Body__:

    {
        "status" : "success",
        "id" : "/root/iplant/trash/home/proxy-user/johnworth",
        "path" : "/iplant/trash/home/proxy-user/johnworth"
    }

__Curl Command__:

    curl http://sample.nibblonian.org/secured/filesystem/user-trash-dir?proxyToken=notReal
