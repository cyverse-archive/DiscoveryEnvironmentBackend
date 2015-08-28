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
        "content-type" : "text/plain",
        "urls" : [],
        "info-type" : "<an info type or empty string>"
    }

The urls field will contain some or none of the following:

* CoGe URLs
* Tree URLS
* anon-files URLs

The URLs are formatted like this:

    {
        "label" : "<LABEL>",
        "url" : "<URL>"
    }

For anonymous URLs, the label will be "anonymous".

For CoGe URLs, the label with start with "gene_".

For tree URLs, the label will usually start with "tree_", but that's not guaranteed (the DE doesn't create the labels).

__Curl Command__:

    curl http://127.0.0.1:3000/secured/filesystem/file/manifest?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt
