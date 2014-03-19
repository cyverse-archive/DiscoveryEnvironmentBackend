Error Codes
-----------

When it encounters an error, filesystem will generally return a JSON object in the form:

    {
        "status" : "failure",
        "error_code" : "ERR_CODE"
    }

Other entries may be included in the map, but you shouldn't depend on them being there for error checking.

