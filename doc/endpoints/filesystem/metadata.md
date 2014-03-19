Metadata
---------------------------

The following commands allow the caller to set and get attributes on files and directories in iRODS. iRODS attributes take the form of Attribute Value Unit triples associated with directories and files. Files/directories cannot have multiple AVUs with the same attribute name, so repeated POSTings of an AVU with the same attribute name will overwrite the old value.


Setting Metadata
------------------------------------
Note the single-quotes around the request URL in the curl command.

__URL Path__: /secured/filesystem/metadata

__HTTP Method__: POST

__Error codes__: ERR_INVALID_JSON, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* path - The iRODS path to the file or directory that the metadata is associated with.

__Request Body__:

    {
        "attr"  : "avu_name",
        "value" : "avu_value",
        "unit"  : "avu_unit"
    }

__Response__:

    {
        "status" : "success",
        "path"   : "\/iplant\/home\/johnw\/LICENSE.txt",
        "user"   : "johnw"
    }

__Curl Command__:

    curl -d '{"attr" : "avu_name", "value" : "avu_value", "unit" : "avu_unit"}' 'http://127.0.0.1:3000/secured/filesystem/metadata?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt'


Setting Metadata as a Batch Operation
-------------------------------------
__URL Path__: /secured/filesystem/metadata-batch

__HTTP Method__: POST

__Error codes__: ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* path - The path to the file or directory being operated on.

__Request Body__:

    {
        "add": [
            {
                "attr"  : "attr",
                "value" : "value",
                "unit"  : "unit"
            },
            {
                "attr"  : "attr1",
                "value" : "value",
                "unit"  : "unit"
            }
        ],
        "delete": [
            {
                "attr"  : "del1",
                "value" : "del1value",
                "unit"  : "del1unit"
            },
            {
                "attr"  : "del2",
                "value" : "del2value",
                "unit"  : "del2unit"
            }
        ]
    }

Both "add" and "delete" lists must be present even if they are empty.

__Response__:

    {
        "status" : "success",
        "path"   : "\/iplant\/home\/wregglej\/LICENSE.txt",
        "user"   :" wregglej"
    }

__Curl Command__:

    curl -d '{"add" : [{"attr" : "attr", "value" : "value", "unit" : "unit"}], "delete" : ["del1", "del2"]}' 'http://127.0.0.1:3000/secured/filesystem/metadata-batch?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt'


Getting Metadata
------------------------------------
__URL Path__: /secured/filesystem/metadata

__HTTP Method__: GET

__Error codes__: ERR_DOES_NOT_EXIST, ERR_NOT_READABLE, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* path - The path to the file or directory being operated on.

__Response__:

    {
        "status": "success",
        "metadata": [
            {
                 "attr": "avu_name",
                 "value": "avu_value",
                 "unit": "avu_unit"
            }
        ]
    }

__Curl Command__:

    curl 'http://127.0.0.1:3000/secured/filesystem/metadata?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt'


Deleting File and Directory Metadata
------------------------------------
__URL Path__: /secured/filesystem/metadata

__HTTP Method__: DELETE

__Error Codes__: ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* path - The path to the file or directory being operated on.

__Response__:

    {
        "status":"success",
        "path":"\/iplant\/home\/johnw\/LICENSE.txt",
        "user":"johnw"
    }

__Curl Command__:

    curl -X DELETE 'http://127.0.0.1:3000/secured/filesystem/metadata?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt&attr=avu_name'


Listing Metadata Templates
--------------------------
__URL Path__: /secured/filesystem/metadata/templates

__HTTP Method__: GET

__Response__:

    {
        "metadata_templates": [
            {
                "id": "59bd3d26-34d5-4e75-99f5-840a20089caf",
                "name": "iDS Genome Sequences"
            }
        ],
        "success": true
    }

__Curl Command__:

    curl -s "http://services-2:31325/secured/filesystem/metadata/templates?proxyToken=notReal"


Viewing a Metadata Template
---------------------------
__URL Path__: /secured/filesystem/metadata/template/:template_id

__HTTP Method__: GET

__Error Codes__: ERR_NOT_FOUND

__Response__:

    {
        "attributes": [
            {
                "description": "project name",
                "id": "33e3e3d8-cd48-4572-8b16-89207b1609ec",
                "name": "project",
                "required": true,
                "synonyms": [],
                "type": "String"
            },
            ...
        ],
        "id": "59bd3d26-34d5-4e75-99f5-840a20089caf",
        "name": "iDS Genome Sequences",
        "success": true
    }

__Curl Command__:

    curl -s "http://services-2:31325/secured/filesystem/metadata/template/59bd3d26-34d5-4e75-99f5-840a20089caf?proxyToken=notReal"

Viewing a Metadata Attribute
----------------------------
__URL Path__: /secured/filesystem/metadata/template/attr/:attribute_id

__HTTP Method__: GET

__Error Codes__: ERR_NOT_FOUND

__Response__:

    {
        "description": "project name",
        "id": "33e3e3d8-cd48-4572-8b16-89207b1609ec",
        "name": "project",
        "required": true,
        "success": true,
        "synonyms": [],
        "type": "String"
    }

__Curl Command__:

    curl -s "http://localhost:31325/secured/filesystem/metadata/template/attr/33e3e3d8-cd48-4572-8b16-89207b1609ec?proxyToken=notReal"
