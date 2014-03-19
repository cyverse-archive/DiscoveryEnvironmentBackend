# Table of Contents

* [Sharing Endpoints](#sharing-endpoints)
    * [Sharing User Data](#sharing-user-data)
    * [Unsharing User Data](#unsharing-user-data)

# Sharing Endpoints

## Sharing User Data

Secured Endpoint: POST /secured/share

This service can be used to share a user's files and folders with other users,
and with specific permissions for each user and resource.

Here's an example:

```
$ curl -sd '
{
    "sharing": [
        {
            "user": "shared-with-user1",
            "paths": [
                {
                    "path": "/path/to/shared/file",
                    "permissions": {
                        "read": true,
                        "write": true,
                        "own": false
                    }
                },
                {
                    "path": "/path/to/shared/folder",
                    "permissions": {
                        "read": true,
                        "write": false,
                        "own": false
                    }
                }
            ]
        },
        {
            "user": "shared-with-user2",
            "paths": [
                {
                    "path": "/path/to/shared/file",
                    "permissions": {
                        "read": true,
                        "write": true,
                        "own": true
                    }
                },
                {
                    "path": "/path/to/shared/folder",
                    "permissions": {
                        "read": true,
                        "write": true,
                        "own": true
                    }
                }
            ]
        }
    ]
}
' "http://by-tor:8888/secured/share?proxyToken=$(cas-ticket)"
```

The service will respond with a success or failure message per user and resource:

```
{
    "sharing": [
        {
            "user": "shared-with-user1",
            "sharing": [
                {
                    "success": true,
                    "path": "/path/to/shared/file",
                    "permissions": {
                        "read": true,
                        "write": true,
                        "own": false
                    }
                },
                {
                    "success": false,
                    "error": {
                        "status": "failure",
                        "action": "share",
                        "error_code": "ERR_DOES_NOT_EXIST",
                        "paths": [
                            "/path/to/shared/folder"
                        ]
                    },
                    "path": "/path/to/shared/folder",
                    "permissions": {
                        "read": true,
                        "write": false,
                        "own": false
                    }
                }
            ]
        },
        {
            "user": "shared-with-user2",
            "sharing": [
                {
                    "success": true,
                    "path": "/path/to/shared/file",
                    "permissions": {
                        "read": true,
                        "write": true,
                        "own": true
                    }
                },
                {
                    "success": false,
                    "error": {
                        "status": "failure",
                        "action": "share",
                        "error_code": "ERR_DOES_NOT_EXIST",
                        "paths": [
                            "/path/to/shared/folder"
                        ]
                    },
                    "path": "/path/to/shared/folder",
                    "permissions": {
                        "read": true,
                        "write": true,
                        "own": true
                    }
                }
            ]
        }
    ]
}
```

## Unsharing User Data

Secured Endpoint: POST /secured/unshare

This service can be used to unshare a user's files and folders with other users.

Here's an example:

```
$ curl -sd '
{
    "unshare": [
        {
            "user": "shared-with-user1",
            "paths": [
                "/path/to/shared/file",
                "/path/to/shared/foo"
            ]
        },
        {
            "user": "shared-with-user2",
            "paths": [
                "/path/to/shared/file",
                "/path/to/shared/folder"
            ]
        }
    ]
}
' "http://by-tor:8888/secured/unshare?proxyToken=$(cas-ticket)"
```

The service will respond with a success or failure message per user:

```
{
    "unshare": [
        {
            "user": "shared-with-user1",
            "unshare": [
                {
                    "success": true,
                    "path": "/path/to/shared/file"
                },
                {
                    "success": false,
                    "error": {
                        "status": "failure",
                        "action": "unshare",
                        "error_code": "ERR_DOES_NOT_EXIST",
                        "paths": [
                            "/path/to/shared/foo"
                        ]
                    },
                    "path": "/path/to/shared/foo"
                }
            ]
        },
        {
            "user": "shared-with-user2",
            "unshare": [
                {
                    "success": true,
                    "path": "/path/to/shared/file"
                },
                {
                    "success": true,
                    "path": "/path/to/shared/folder"
                }
            ]
        }
    ]
}
```
