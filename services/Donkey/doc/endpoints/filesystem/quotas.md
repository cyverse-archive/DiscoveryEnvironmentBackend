Listing a user's quotas
-----------------------

__URL Path__: /secured/filesystem/quota

__HTTP Method__: GET

__Error Codes__: ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Response Body__:

    {
        "status" : "success",
        "quotas" : [
            {
                "zone" : "iplant",
                "resource" : "demoResc",
                "over" : "-1000000",
                "updated" : "1341611109000",
                "limit" : "1000000",
                "user" : "testuser"
            }
        ]
    }

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/quota?proxyToken=notReal
