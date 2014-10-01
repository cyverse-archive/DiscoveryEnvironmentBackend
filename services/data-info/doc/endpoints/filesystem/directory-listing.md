Directory List (Non-Recursive)
------------------------------

Only lists subdirectories of the directory path passed into it.

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
            "filter": false,
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
            "filter": false,
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
            "filter": false,
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
    "filter": true,
    "permission": "own"
    "success": true
    }

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/directory?proxyToken=notReal


Paged Directory Listing
-----------------------

Provides a paged directory listing for large directories. Always includes files (unless the directory doesn't contain any).

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
* sort-col - The column to sort the result set by. Sorting is done in iRODS's ICAT database, not at the application level. Accepted values are NAME, ID, LASTMODIFIED, DATECREATED, SIZE, PATH. The values are case-insensitive.
* sort-order - The order to sort the result set in. Accepted values are ASC and DESC. The values are case-insensitive.

__Response Body__:

    {
        "date-created": 1369778522000,
        "date-modified": 1379520049000,
        "file-size": 0,
        "files": [
            {
                "date-created": 1379519492000,
                "date-modified": 1379520049000,
                "file-size": 196903039,
                "id": "0d880c78-df8a-11e3-bfa5-6abdce5a08d5",
                "path": "/iplant/home/wregglej/centos-5.8-x86-64-minimal.box",
                "label": "centos-5.8-x86-64-minimal.box",
                "isFavorite" : false,
                "permission": "own"
            }
        ],
        "folders": [
            {
                "date-created": 1374080225000,
                "date-modified": 1374080225000,
                "file-size": 0,
                "hasSubDirs": true,
                "id": "6375efce-e061-11e3-bfa5-6abdce5a08d5",
                "path": "/iplant/home/wregglej/asdfafa",
                "label": "asdfafa",
                "isFavorite" : false,
                "permission": "own"
            },
            {
                "date-created": 1377814242000,
                "date-modified": 1377814242000,
                "file-size": 0,
                "hasSubDirs": true,
                "id": "b4987bf4-e063-11e3-bfa5-6abdce5a08d5",
                "path": "/iplant/home/wregglej/asdf bar",
                "label": "asdf bar",
                "isFavorite" : false,
                "permission": "own"
            },
            {
                "date-created": 1373397344000,
                "date-modified": 1377558112000,
                "file-size": 0,
                "hasSubDirs": true,
                "id" : "0d622cd8-df8a-11e3-bfa5-6abdce5a08d5",
                "path": "/iplant/home/wregglej/Find_Unique_Values_analysis1-2013-07-09-12-15-37.024",
                "label": "Find_Unique_Values_analysis1-2013-07-09-12-15-37.024",
                "isFavorite" : false,
                "permission": "own"
            },
            {
                "date-created": 1374080529000,
                "date-modified": 1374080529000,
                "file-size": 0,
                "hasSubDirs": true,
                "id": "0d627292-df8a-11e3-bfa5-6abdce5a08d5",
                "path": "/iplant/home/wregglej/zaaaaaaaa",
                "label": "zaaaaaaaa",
                "isFavorite" : false,
                "permission": "own"
            }
        ],
        "hasSubDirs": true,
        "id": "16426b48-e128-11e3-9076-6abdce5a08d5",
        "path": "/iplant/home/wregglej",
        "label": "wregglej",
        "isFavorite" : false,
        "permission": "own",
        "success": true,
        "total" : 218
    }

__Curl Command__:

    curl "http://127.0.0.1:31325/secured/filesystem/paged-directory?proxyToken=asdfadsfa&path=/iplant/home/wregglej&sort-col=SIZE&sort-order=DESC&limit=5&offset=10"
