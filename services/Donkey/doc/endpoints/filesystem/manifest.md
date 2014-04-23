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
        "urls" : [],
        "info-type" : "<an info type or empty string>"
        "mime-type" : "<a valid filetype>"
    }

The urls field will contain some or none of the following:

* CoGe URLs
* Tree URLS
* anon-files URLs

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/file/manifest?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt
