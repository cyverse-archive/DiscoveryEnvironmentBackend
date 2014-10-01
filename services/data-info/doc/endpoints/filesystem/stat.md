File and Directory Status Information
-------------------------------------

The /stat endpoint allows the caller to get serveral pieces of information about a file or directory at once.  For directories, the response includes the created and last-modified timestamps along with a file type of `dir`.  For regular files, the response contains the created and last-modified timestamps, the file size in bytes and a file type of `file`.  The following is an example call to the stat endpoint:

__URL Path__: /secured/filesystem/stat

__HTTP Method__: POST

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

```json
{
    "paths": [
         "/iplant/home/wregglej/BTSync.dmg"
    ]
}
```

__Response Body__:
```json
{
    "paths": {
        "/iplant/home/wregglej/BTSync.dmg": {
            "date-created": 1.398183371e+12,
            "date-modified": 1.398183371e+12,
            "file-size": 1.0822742e+07,
            "id": "0dfcac40-df8a-11e3-bfa5-6abdce5a08d5",
            "info-type": "",
            "label": "BTSync.dmg",
            "mime-type": "application/octet-stream",
            "path": "/iplant/home/wregglej/BTSync.dmg",
            "permission": "own",
            "share-count": 1,
            "type": "file"
        }
    },
    "success": true
}
```

Note that entries in the "paths" map that are directories will include "file-count" and "dir-count" fields, while file entries will not.

The "share-count" field is provided for both files and directories and lists the number of users that a file is shared with.

__Curl Command__:

    curl -H "Content-Type:application/json" -sd '{"paths":["/iplant/home/wregglej/BTSync.dmg"]}' http://services-2:31360/secured/filesystem/stat?proxyToken=notReal



