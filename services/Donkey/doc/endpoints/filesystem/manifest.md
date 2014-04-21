File manifest
-------------

__URL Path__: /secured/filesystem/file/manifest

__HTTP Method__: GET

__Error Codes__: ERR_DOES_NOT_EXIST, ERR_NOT_A_FILE, ERR_NOT_READABLE, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* path - Path to a file in iRODSs.

__Response Body__:

    {
        "status" : "success",
        "content-type" : "text/plain",
        "preview":"file\/preview?user=johnw&path=\/iplant\/home\/johnw\/LICENSE.txt",
        "tree-urls" : [],
        "info-type" : "<an info type or empty string>"
        "mime-type" : "<a valid filetype>"
    }

Or, if the path is readable by the anonymous user:

    {
        "status" : "success",
        "anon-url" : "http://de-2.iplantcollaborative.org/anon/iplant/home/johnw/LICENSE.txt",
        "content-type" : "text/plain",
        "preview":"file\/preview?user=johnw&path=\/iplant\/home\/johnw\/LICENSE.txt",
        "tree-urls" : [],
        "info-type" : "<an info type or empty string>"
        "mime-type" : "<a valid filetype>"
    }

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/file/manifest?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt
