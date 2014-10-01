Listing User Permissions
------------------------

Lists the users that have access to a file and the their permissions on the file. The user making the request and the configured rodsadmin user are filtered out of the returned list. The user making the request must own the file.

__URL Path__: /secured/filesystem/user-permissions

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_DOES_NOT_EXIST, ERR_NOT_OWNER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

```json
{
    "paths": [
        "/iplant/home/testuser/testfile",
        "/iplant/home/testuser/testfile2"
    ]
}
```

__Response Body__:

```json
{
    "status": "success",
    "paths": [
        {
            "path": "/iplant/home/testuser/testfile",
            "user-permissions": [
                {
                    "user": "user1",
                    "permission": "read"
                }
            ]
        },
        {
            "path": "/iplant/home/testuser/testfile2",
            "user-permissions": [
                {
                    "user": "user2",
                    "permission": "read"
                }
            ]
        }
    ]
}
```

__Curl Command__:

    curl -d '{"paths" : ["/iplant/home/testuser/testfile", "/iplant/home/testuser/testfile2"]}' 'http://nibblonian.example.org/secured/filesystem/user-permissions?proxyToken=notReal'
