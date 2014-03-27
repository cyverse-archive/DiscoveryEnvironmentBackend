Sharing
-------

Shares a file or directory with another user. The user being shared with is given read-only access to all of the parent directories as well. This allows the user to drill down to the shared file/directory from the "Shared" section of the data management window.

Some users being shared with may be skipped for various reasons. When this happens, any sharing attempts made for that user will be included in a list of skipped sharing attempts in the response body along with a code indicating the reason the sharing attempt was skipped.

Note that "users" and "paths" are always lists, even if only one user or path is specified.

__URL Path__: /secured/filesystem/share

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_DOES_NOT_EXIST, ERR_NOT_OWNER

__Request Query Parameters__:
* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : ["/path/to/shared/file"],
        "users" : ["shared-with-user"],
        "permission": "write"
    }

__Response Body__:

    {
        "status" : "success",
        "user" : ["users shared with"],
        "path" : ["the paths that were shared"],
        "permission": "write",
        "skipped" : [
            {
                "path" : "/path/to/shared/file",
                "reason" : "share-with-self",
                "user" : "fileowner"
            }
        ]
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"paths" : ["/path/to/shared/file"], "users" : ["shared-with-user1", "fileowner"], "permission": "write"}' http://nibblonian.yourhostname.org/secured/filesystem/share?proxyToken=notReal



Unsharing
------------------------
Unshares a file or directory. All ACLs for the specified user are removed from the file or directory. To simply change existing ACLs, recall the /share end-point with the desired permissions.

Some users may be skipped for various reasons.  When this happens, any unsharing attempts made for that user will be included in a list of skipped unsharing attempts in the response body along with a code indicating the reason the unsharing attempt was skipped.

Note that "users" and "paths" are always lists, even if only one user or path is specified.

__URL Path__: /secured/filesystem/unshare

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_DOES_NOT_EXIST, ERR_NOT_OWNER

__Request Body__:

    {
        "paths" : ["/path/to/shared/file"],
        "users" : ["shared-with-user"]
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"paths" : ["/path/to/shared/file"], "users" : ["shared-with-user", "fileowner"]}' http://nibblonian.yourhostname.org/secured/filesystem/unshare?proxyToken=notReal

__Response Body__:

    {
        ":status" : "success",
        "path : ["/path/to/shared/file"],
        "user" : ["shared-with-user"],
        "skipped" : [
            {
                "path" : "/path/to/shared/file",
                "reason" : "unshare-with-self",
                "user" : "fileowner"
            }
        ]
    }
