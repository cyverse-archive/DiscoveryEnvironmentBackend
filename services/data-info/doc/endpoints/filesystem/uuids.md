Paths for UUIDS
---------------------------------

This endpoint accepts UUIDS and returns JSON objects for the paths similar to the objects returned by the stat endpoints.

__URL Path__: /secured/filesystem/paths-for-uuids

__HTTP Method__: POST

__Error Codes__: ERR_TOO_MANY_RESULTS, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD

The ERR_TOO_MANY_RESULTS error should be rare, it's only thrown if a file or folder ends up with multiple UUIDS associated with it.

__Request Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "uuids" : ["0e66f708-df8a-11e3-bfa5-6abdce5a08d5"]
    }

__Response__:

```json
{
    "paths": [
        {
            "date-created": 1.399049686e+12,
            "date-modified": 1.399049686e+12,
            "file-size": 176,
            "id": "0e66f708-df8a-11e3-bfa5-6abdce5a08d5",
            "info-type": "newick",
            "label": "boo.newick",
            "mime-type": "text/plain",
            "path": "/iplant/home/wregglej/boo.newick",
            "permission": "own",
            "share-count": 0,
            "type": "file"
        },
        {
            "date-created": 1.398113126e+12,
            "date-modified": 1.398113126e+12,
            "file-size": 2461,
            "id": "0d98745a-df8a-11e3-bfa5-6abdce5a08d5",
            "info-type": "bash",
            "label": "iplant.sh",
            "mime-type": "application/x-sh",
            "path": "/iplant/home/wregglej/iplant.sh",
            "permission": "own",
            "share-count": 1,
            "type": "file"
        }
    ]
}
```

This response differs from the stat endpoint because of the added "uuid" field.  
   
__Curl Command__:

    curl -d '{"uuids" : ["0e66f708-df8a-11e3-bfa5-6abdce5a08d5", "0d98745a-df8a-11e3-bfa5-6abdce5a08d5"]}' http://localhost:31325/secured/filesystem/paths-for-uuids?proxyToken=asdfasdf | squiggles


UUIDS for paths
--------------

This endpoint accepts paths and returns JSON objects similar to those returned by the stat endpoint but with a "uuid" field added in.

__URL Path__: /secured/filesystem/uuids-for-paths

__HTTP Method__: POST

__Error codes__: ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_USER, ERR_NOT_FOUND

The ERR_NOT_FOUND error should be rare and will pop up when a file does not have a UUID associated with it.

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : [
            "/iplant/home/wregglej/boo.newick", 
            "/iplant/home/wregglej/iplant.sh"
        ]
    }

__Response__:

```json
{
    "paths": [
        {
            "date-created": 1.399049686e+12,
            "date-modified": 1.399049686e+12,
            "file-size": 176,
            "id": "0e66f708-df8a-11e3-bfa5-6abdce5a08d5",
            "info-type": "newick",
            "label": "boo.newick",
            "mime-type": "text/plain",
            "path": "/iplant/home/wregglej/boo.newick",
            "permission": "own",
            "share-count": 0,
            "type": "file"
        },
        {
            "date-created": 1.398113126e+12,
            "date-modified": 1.398113126e+12,
            "file-size": 2461,
            "id": "0d98745a-df8a-11e3-bfa5-6abdce5a08d5",
            "info-type": "bash",
            "label": "iplant.sh",
            "mime-type": "application/x-sh",
            "path": "/iplant/home/wregglej/iplant.sh",
            "permission": "own",
            "share-count": 1,
            "type": "file" 
        }
    ]
}
```

__Curl Command__:

    curl -d '{"paths" : ["/iplant/home/wregglej/boo.newick", "/iplant/home/wregglej/iplant.sh"]}' http://localhost:31325/secured/filesystem/uuids-for-paths?proxyToken=asdfasdf | squiggles
