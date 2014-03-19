Listing a user's group memberships
----------------------------------

This endpoint provides access to the list of a groups a given iRODS user belongs
to.

__URL Path__: /secured/filesystem/groups

__HTTP Method__: GET

__Error Codes__: ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Response Body__:

    {
        "status" : "success",
        "groups" : ["group1", "group2"]
    }

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/groups?proxyToken=notReal
