File Download
-------------
Downloads are now handled by iDrop Lite. Filesystem handles serializing the shopping cart object and returning a temporary password.

__URL Path__: /secured/filesystem/download

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : ["/tempZone/home/muahaha/test.txt"]
    }

__Response Body__:

    {
        "status":"success",
        "data": {
                    "user":"muahaha",
                    "password":"cc181a5a97635c7b45a3b2b828f964fe",
                    "host":"blowhole.example.org",
                    "port":1247,
                    "zone":"tempZone",
                    "defaultStorageResource":"",
                    "key":"1325878326128"
                }
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"paths":["/tempZone/home/muahaha/test.txt"]}' 'http://nibblonian.example.org/secured/filesystem/download?proxyToken=notReal


Downloading all items in a Directory
-------------

All items in a directory may be downloaded by iDrop Lite.
Filesystem handles serializing the shopping cart object with each item under the given directory and returning a temporary password for this cart.

__URL Path__: /secured/filesystem/download-contents

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_NOT_A_FOLDER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "path": "/tempZone/home/tester/test"
    }

__Response Body__:

    {
        "status":"success",
        "data": {
                    "user":"tester",
                    "password":"cc181a5a97635c7b45a3b2b828f964fe",
                    "host":"de.example.org",
                    "port":1247,
                    "zone":"tempZone",
                    "defaultStorageResource":"",
                    "key":"1325878326128"
                }
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"path": "/tempZone/home/tester/test"}' 'http://nibblonian.example.org/secured/filesystem/download-contents?proxyToken=notReal
