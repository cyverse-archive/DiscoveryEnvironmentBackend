Top Level Root Listing
----------------------

Delegates to data-info: `GET /navigation/root`

This endpoint is a passthrough to the data-info endpoint above,
though it will add the `label` and `hasSubDirs` fields to data-info responses.
Please see the data-info documentation for more information.

__URL Path__: /secured/filesystem/root

__HTTP Method__: GET

__Request Query Parameters__:
* proxyToken - A valid CAS ticket.

__Response Body__:

```json
{
    "roots": [
        {
            "date-modified": 1340918988000,
            "hasSubDirs": true,
            "permission": "own",
            "date-created": 1335217160000,
            "label": "wregglej",
            "id": "0b331f99-896f-4465-b0bf-15185c53414c",
            "path": "/iplant/home/wregglej"
        },
        {
            "date-modified": 1335476028000,
            "hasSubDirs": true,
            "permission": "write",
            "date-created": 1335217387000,
            "label": "Community Data",
            "id": "54ab8910-f9b3-11e4-9d60-1a5a300ff36f",
            "path": "/iplant/home/shared"
        }
    ]
}
```

__Curl Command__:

    curl 'http://127.0.0.1::3000/secured/filesystem/root?proxyToken=notReal'
