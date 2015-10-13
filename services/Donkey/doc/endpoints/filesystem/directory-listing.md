Directory List (Non-Recursive)
------------------------------

Only lists subdirectories of the directory path passed into it.
Delegates to the `POST /favorites/filter` metadata endpoint in order to set the `isFavorite` flags
in the response. If the metadata service is not available, then these flags will be set to `false`
by default.

__URL Path__: /secured/filesystem/directory

__HTTP Method__: GET

__Error Codes__: ERR_NOT_A_USER, ERR_NOT_READABLE

__Request Query Params__:

* proxyToken - A valid CAS ticket.
* path - The path to list. Optional. If ommitted, the user's home directory is used.

__Response Body__:

    {
    "date-created": 1369778522000,
    "date-modified": 1381177547000,
    "file-size": 0,
    "folders": [
        {
            "date-created": 1373927956000,
            "date-modified": 1374015533000,
            "file-size": 0,
            "badName": false,
            "hasSubDirs": true,
            "path": "/iplant/home/wregglej/acsxfdqswfrdafds",
            "label": "acsxfdqswfrdafds",
            "isFavorite" : false,
            "id": "0c3eb574-df8a-11e3-bfa5-6abdce5a08d5",
            "permission": "own"
        },
        {
            "date-created": 1371157127000,
            "date-modified": 1380909580000,
            "file-size": 0,
            "badName": false,
            "hasSubDirs": true,
            "path": "/iplant/home/wregglej/analyses",
            "label": "analyses",
            "isFavorite" : false,
            "id": "1c2c436c-e128-11e3-9087-6abdce5a08d5",
            "permission": "own"
        },
        {
            "date-created": 1380814985000,
            "date-modified": 1380814985000,
            "file-size": 0,
            "badName": false,
            "hasSubDirs": true,
            "path": "/iplant/home/wregglej/analyses3",
            "label": "analyses3",
            "isFavorite" : false,
            "id": "1f293516-e128-11e3-9087-6abdce5a08d5",
            "permission": "own"
        },

    ],
    "hasSubDirs": true,
    "id": "a3794158-df89-11e3-bf7d-6abdce5a08d5",
    "path": "/iplant/home/wregglej",
    "label": "wregglej",
    "isFavorite" : false,
    "badName": true,
    "permission": "own"
    }

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/directory?proxyToken=notReal


Paged Directory Listing
-----------------------

Provides a paged directory listing for large directories. Always includes files (unless the directory doesn't contain any).
Delegates to the `POST /favorites/filter` metadata endpoint in order to set the `isFavorite` flags
in the response. If the metadata service is not available, then these flags will be set to `false`
by default.

__URL Path__: /secured/filesystem/paged-directory

__HTTP Method__: GET

__Error Codes__:

* ERR_NOT_A_USER
* ERR_NOT_READABLE
* ERR_NOT_A_FOLDER
* ERR_NOT_READABLE
* ERR_DOES_NOT_EXIST
* ERR_INVALID_SORT_COLUMN
* ERR_INVALID_SORT_ORDER

__Request Query Params__:

* proxyToken - A valid CAS ticket.
* path - The path to list. Must be a directory.
* limit - The total number of results to return in a page. This is the number of folders and files combined.
* offset - The offset into the directory listing result set to begin the listing at.
* entity-type - (OPTIONAL) The type of entity to return, FILE, FOLDER, or ANY. The values are case-insensitive. It defaults to ANY.
* info-type - (OPTIONAL) Filter the files portion of the result set so that only files with this info type are returned. To return multiple info types, and this parameter more than once.
* sort-col - The column to sort the result set by. Sorting is done in iRODS's ICAT database, not at the application level. Accepted values are NAME, ID, LASTMODIFIED, DATECREATED, SIZE, PATH. The values are case-insensitive.
* sort-dir - The order to sort the result set in. Accepted values are ASC and DESC. The values are case-insensitive.


__Response Body__:

    {
        "badName": false,
        "date-created": 1369778522000,
        "date-modified": 1379520049000,
        "file-size": 0,
        "files": [
            {
                "badName": false,
                "date-created": 1379519492000,
                "date-modified": 1379520049000,
                "file-size": 196903039,
                "id": "0d880c78-df8a-11e3-bfa5-6abdce5a08d5",
                "infoType": null,
                "path": "/iplant/home/wregglej/centos-5.8-x86-64-minimal.box",
                "label": "centos-5.8-x86-64-minimal.box",
                "isFavorite" : false,
                "permission": "own"
            }
        ],
        "folders": [
            {
                "badName": false,
                "date-created": 1374080225000,
                "date-modified": 1374080225000,
                "file-size": 0,
                "hasSubDirs": true,
                "id": "6375efce-e061-11e3-bfa5-6abdce5a08d5",
                "infoType": null,
                "path": "/iplant/home/wregglej/asdfafa",
                "label": "asdfafa",
                "isFavorite" : false,
                "permission": "own"
            },
            {
                "badName": false,
                "date-created": 1377814242000,
                "date-modified": 1377814242000,
                "file-size": 0,
                "hasSubDirs": true,
                "id": "b4987bf4-e063-11e3-bfa5-6abdce5a08d5",
                "infoType": null,                
                "path": "/iplant/home/wregglej/asdf bar",
                "label": "asdf bar",
                "isFavorite" : false,
                "permission": "own"
            },
            {
                "badName": false,
                "date-created": 1373397344000,
                "date-modified": 1377558112000,
                "file-size": 0,
                "hasSubDirs": true,
                "id" : "0d622cd8-df8a-11e3-bfa5-6abdce5a08d5",
                "infoType": null,                
                "path": "/iplant/home/wregglej/Find_Unique_Values_analysis1-2013-07-09-12-15-37.024",
                "label": "Find_Unique_Values_analysis1-2013-07-09-12-15-37.024",
                "isFavorite" : false,
                "permission": "own"
            },
            {
                "badName": false,
                "date-created": 1374080529000,
                "date-modified": 1374080529000,
                "file-size": 0,
                "hasSubDirs": true,
                "id": "0d627292-df8a-11e3-bfa5-6abdce5a08d5",
                "infoType": null,                
                "path": "/iplant/home/wregglej/zaaaaaaaa",
                "label": "zaaaaaaaa",
                "isFavorite" : false,
                "permission": "own"
            }
        ],
        "hasSubDirs": true,
        "id": "16426b48-e128-11e3-9076-6abdce5a08d5",
        "infoType": null,                
        "path": "/iplant/home/wregglej",
        "label": "wregglej",
        "isFavorite" : false,
        "permission": "own",
        "total": 218,
        "totalBad": 0
    }

__Curl Command__:

    curl "http://127.0.0.1:31325/secured/filesystem/paged-directory?proxyToken=asdfadsfa&path=/iplant/home/wregglej&sort-col=SIZE&sort-dir=DESC&limit=5&offset=10"
