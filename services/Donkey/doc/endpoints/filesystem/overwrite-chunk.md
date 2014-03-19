Overwriting a chunk of a file
-------------------------

__URL Path__: /secured/filesystem/overwrite-chunk

__HTTP Method__: POST

__Error Codes___: ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_FILE, ERR_NOT_A_USER

__Request Query Parameters___:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "path"       : "/iplant/home/wregglej/testfile",
        "position"   : "20",
        "update"     : "I am an update"
    }

__Response__:

    {
        "status"     : "success",
        "path"       : "/iplant/home/wregglej/testfile",
        "user"       : "wregglej",
        "start"      : "20",
        "chunk-size" : "14",
        "file-size"  : "34"
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"path" : "/iplant/home/wregglej/testfile", "position" : "20", "update" : "I am an update"}' http://127.0.0.1:31360/secured/filesystem/overwrite-chunk?proxyToken=notReal

Notes:
* 'position' is in bytes.
* 'position' must be parseable as longs.
* 'start', 'chunk-size', and 'file-size' in the response are all in bytes.
* 'start', 'chunk-size', amd 'file-size' in the response should all be parseable as longs.
* The byte at 'position' is not included in the overwrite. The overwrite begins at position + 1.
