# File Type Endpoints

NOTES: If a path appears in a query string parameter, URL encode it first.

Error code maps follow the general format of `data-info`'s errors:

    {
        err_code: "ERR_CODE_STRING",
 		...
    }

Currently Supported File Types
------------------------------

Refer to heuristomancer's documentation, or make a request against the endpoint below.


Get the list of supported file types
---------------------------------------

This endpoint delegates to data-info's GET /file-types endpoint.

__URL Path__: /secured/filetypes/type-list

__HTTP Method__: GET

__Error Codes__: None

__Request Query Parameters__:
* proxyToken - A valid CAS proxy token.

__Response Body__:

    {
        "types" : ["csv, "tsv"]
    }

__Curl Command__:

    curl http://donkey.example.org:31325/secured/filetypes/type-list?proxyToken=notARealOne


Add/update/unset a file type of a file
--------------------------------------

This endpoint delegates (after looking up a UUID for the file path provided) to data-info's PUT /data/:data-id/type endpoint.

__URL Path__: /secured/filetypes/type

__HTTP Method__: POST

__Error Codes__: ERR_NOT_OWNER, ERR_BAD_OR_MISSING_FIELD, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_NOT_A_FILE

__Request Query Parameters__:
* proxyToken - A valid CAS proxy token.

__Request Body__:

    {
        "path" : "/path/to/irods/file",
        "type" : "csv"
    }
If you want to reset the type so that it's blank, pass in a empty string for the type, like in the following:

    {
        "path" : "/path/to/irods/file",
        "type" : ""
    }

__Response Body__:

    {
        "path" : "/path/to/irods/file",
        "type" : "csv"
    }

__Curl Command__:

    curl -d '{"path" : "/path/to/irods/file","type":"csv"}' 'http://donkey.example.org:31325/secured/filetypes/type?proxyToken=notARealOne'
