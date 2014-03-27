File and Directory Status Information
-------------------------------------

The /stat endpoint allows the caller to get serveral pieces of information about a file or directory at once.  For directories, the response includes the created and last-modified timestamps along with a file type of `dir`.  For regular files, the response contains the created and last-modified timestamps, the file size in bytes and a file type of `file`.  The following is an example call to the stat endpoint:

__URL Path__: /secured/filesystem/stat

__HTTP Method__: POST

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : [
            "/iplant/home/dennis/foo",
            "/iplant/home/dennis/foo.txt",
            "/iplant/home/dennis/foo.bar"
        ]
    }

__Response Body__:

    {
        "paths": {
            "/iplant/home/dennis/foo": {
                "share-count" : 0,
                "permission" : "own",
                "dir-count" : 3,
                "file-count" : 4,
                "created": "1339001248000",
                "modified": "1339001248000",
                "type": "dir"
            },
            "/iplant/home/dennis/foo.bar": null,
            "/iplant/home/dennis/foo.txt": {
                "share-count" : 0,
                "permission" : "own",
                "created": "1335289356000",
                "modified": "1335289356000",
                "size": 4,
                "type": "file",
                "info-type" : "<an info type or empty string>"
                "mime-type" : "<a valid filetype>"
            }
        },
        "status": "success"
    }

Note that entries in the "paths" map that are directories will include "file-count" and "dir-count" fields, while file entries will not.

The "share-count" field is provided for both files and directories and lists the number of users that a file is shared with.

__Curl Command__:

    curl -H "Content-Type:application/json" -sd '{"paths":["/iplant/home/dennis/foo","/iplant/home/dennis/foo.txt","/iplant/home/dennis/foo.bar"]}' http://services-2:31360/secured/filesystem/stat?proxyToken=notReal



