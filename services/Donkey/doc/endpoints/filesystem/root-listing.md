Top Level Root Listing
----------------------

This endpoint provides a shortcut for the front-end to list the top-level directories (i.e. the user's home directory and Community Data).

__URL Path__: /secured/filesystem/root

__HTTP Method__: GET

__Error Codes__: ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_USER

__Request Query Parameters__:
* proxyToken - A valid CAS ticket.

__Response Body__:

```json
{
    "action": "root",
    "status": "success",
    "roots": [
        {
            "date-modified": "1340918988000",
            "hasSubDirs": true,
            "permission": "own",
            "date-created": "1335217160000",
            "label": "wregglej",
            "id": "/root/iplant/home/wregglej",
            "path": "/iplant/home/wregglej"
        },
        {
            "date-modified": "1335476028000",
            "hasSubDirs": true,
            "permission": "write",
            "date-created": "1335217387000",
            "label": "Community Data",
            "id": "/root/iplant/home/shared",
            "path": "/iplant/home/shared"
        }
    ]
}
```

__Curl Command__:

    curl 'http://127.0.0.1::3000/secured/filesystem/root?proxyToken=notReal'


