File Upload
-----------
Uploads are now handled by iDrop Lite. `data-info` is only responsible for generating a temporary 
password for a user and returning connection information.

__URL Path__: /secured/filesystem/upload

__HTTP Method__: GET

__Error Codes__: ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS proxy ticket.

__Response Body__:

    {
        "action":"upload",
        "status":"success",
        "data": {
                    "user":"muahaha",
                    "password":"c5dbff21fa123d5c726f27cff8279d70",
                    "host":"blowhole.example.org",
                    "port":1247,
                    "zone":"tempZone",
                    "defaultStorageResource":"",
                    "key":"1325877857614"
                }
    }

__Curl Command__:

    curl http://nibblonian.example.org/secured/filesystem/upload?proxyToken=notReal


