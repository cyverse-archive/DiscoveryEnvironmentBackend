Replacing Spaces With Underscores
---------------------------------

__URL Path__: /secured/filesystem/replace-spaces

__HTTP Method__: POST

__Error codes__: ERR_DOES_NOT_EXIST, ERR_BAD_OR_MISSING_FIELD, ERR_NOT_OWNER, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : [
            "/this/is a/path",
            "/this/is a/path with spaces"
        ]
    }

"paths" must contain existing paths.

__Response Body__:

    {
        "status" : "success",
        "paths" : {
            "\/this\/is a\/path" : "\/this\/is_a\/path",
            "\/this\/is a\/path with spaces" : "\/this\/is_a\/path_with_spaces"
        }
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '{"paths" : ["/this/is a/path", "/this/is a/path with spaces"]}' http://127.0.0.1:3000/secured/filesystem/replace-spaces?proxyToken=notReal