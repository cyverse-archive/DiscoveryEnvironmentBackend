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

    curl -s "http://127.0.0.1:3000/secured/filesystem/metadata/templates?proxyToken=notReal"


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

    curl -s "http://127.0.0.1:3000/secured/filesystem/metadata/template/59bd3d26-34d5-4e75-99f5-840a20089caf?proxyToken=notReal"

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

    curl -s "http://127.0.0.1:3000/secured/filesystem/metadata/template/attr/33e3e3d8-cd48-4572-8b16-89207b1609ec?proxyToken=notReal"

Viewing all Metadata Template AVUs on a File/Folder
-----------------------------------------------------
__URL Path__: /secured/filesystem/:data-id/template-avus

__HTTP Method__: GET

__Error Codes__: ERR_NOT_READABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

```json
{
    "success": true,
    "data_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
    "templates": [
        {
            "template_id": "40ac191f-bb36-4f4e-85fb-8b50abec8e10",
            "avus": [
                {
                    "id": "59e55613-fc65-4714-b40e-6d4068b63b82",
                    "attr": "submitted to insdc",
                    "value": "true",
                    "unit": "",
                    "target_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
                    "created_by": "ipctest",
                    "modified_by": "ipctest",
                    "created_on": 1402967025701,
                    "modified_on": 1402968064927
                },
                {
                    "id": "45dba651-b477-4900-bcd7-b1d88995840d",
                    "attr": "project name",
                    "value": "CORE-5602",
                    "unit": "",
                    "target_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
                    "created_by": "ipctest",
                    "modified_by": "ipctest",
                    "created_on": 1402967025701,
                    "modified_on": 1402968064980
                },
                {
                    "id": "74719265-0728-4ec7-81aa-7df77ffbc936",
                    "attr": "Metadata complete",
                    "value": "false",
                    "unit": "",
                    "target_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
                    "created_by": "ipctest",
                    "modified_by": "ipctest",
                    "created_on": 1402967025701,
                    "modified_on": 1402968065030
                }
            ]
        },
        {
            "template_id": "...",
            "avus": [ ... ]
        }
    ]
}
```

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus?proxyToken=notReal"

Viewing a Metadata Template's AVUs on a File/Folder
---------------------------------------------------------
__URL Path__: /secured/filesystem/:data-id/template-avus/:template-id

__HTTP Method__: GET

__Error Codes__: ERR_NOT_READABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

```json
{
    "success": true,
    "data_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
    "user": "ipctest",
    "template_id": "40ac191f-bb36-4f4e-85fb-8b50abec8e10",
    "avus": [
        {
            "id": "59e55613-fc65-4714-b40e-6d4068b63b82",
            "attr": "submitted to insdc",
            "value": "true",
            "unit": "",
            "target_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
            "created_by": "ipctest",
            "modified_by": "ipctest",
            "created_on": 1402967025701,
            "modified_on": 1402968064927
        },
        {
            "id": "45dba651-b477-4900-bcd7-b1d88995840d",
            "attr": "project name",
            "value": "CORE-5602",
            "unit": "",
            "target_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
            "created_by": "ipctest",
            "modified_by": "ipctest",
            "created_on": 1402967025701,
            "modified_on": 1402968064980
        },
        {
            "id": "74719265-0728-4ec7-81aa-7df77ffbc936",
            "attr": "Metadata complete",
            "value": "false",
            "unit": "",
            "target_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
            "created_by": "ipctest",
            "modified_by": "ipctest",
            "created_on": 1402967025701,
            "modified_on": 1402968065030
        }
    ]
}
```

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10?proxyToken=notReal"

Adding and Updating Metadata Template AVUs on a File/Folder
-----------------------------------------------------------------
Including an existing AVU’s ID in its JSON in the POST body will update its values and modified_on
timestamp, and also ensure that the AVU is associated with the metadata template. AVUs included
without an ID will be added to the data item if the AVU does not already exist, otherwise the AVU
with matching attr, owner, and target is updated as previously described.

AVUs can only be associated with one metadata template per data item, per user. All AVUs on the given data item will be disaccociated with all other Metadata Templates.

__URL Path__: /secured/filesystem/:data-id/template-avus/:template-id

__HTTP Method__: POST

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD

__Request Body__:

```json
{
    "avus": [
        {
            "id": "avu-uuid",
            "attr": "attr-1",
            "value": "value-1",
            "unit": "unit-1"
        },
        {
            "attr": "attr-2",
            "value": "value-2",
            "unit": "unit-2"
        },
        {
            "attr": "...",
            "value": "...",
            "unit": "..."
        }
    ]
}
```

__Response__:

```json
{
    "success": true,
    "data_id": "cc20cbf8-df89-11e3-bf8b-6abdce5a08d5",
    "template_id": "40ac191f-bb36-4f4e-85fb-8b50abec8e10",
    "avus": [
        {
            "id": "59e55613-fc65-4714-b40e-6d4068b63b82",
            "attr": "submitted to insdc",
            "value": "true",
            "unit": ""
        },
        {
            "id": "45dba651-b477-4900-bcd7-b1d88995840d",
            "attr": "project name",
            "value": "CORE-5602",
            "unit": ""
        },
        {
            "id": "74719265-0728-4ec7-81aa-7df77ffbc936",
            "attr": "Metadata complete",
            "value": "false",
            "unit": ""
        }
    ]
}
```

__Curl Command__:

```json
curl -sd '
{
    "avus": [
        {
            "attr": "submitted to insdc",
            "unit": "",
            "value": "true"
        },
        {
            "attr": "project name",
            "unit": "",
            "value": "CORE-5602"
        },
        {
            "id": "74719265-0728-4ec7-81aa-7df77ffbc936",
            "attr": "Metadata complete",
            "value": "false",
            "unit": ""
        }
    ]
}
' "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10?proxyToken=notReal"
```

Removing all Metadata Template AVUs on a File/Folder
----------------------------------------------------------
__URL Path__: /secured/filesystem/:data-id/template-avus/:template-id

__HTTP Method__: DELETE

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

```json
{"success":true}
```

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10?proxyToken=notReal"

Removing a Metadata Template AVU from a File/Folder
---------------------------------------------------------
__URL Path__: /secured/filesystem/:data-id/template-avus/:template-id/:avu-id

__HTTP Method__: DELETE

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

```json
{"success":true}
```

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10/b9315be3-5dd4-42bd-9b9f-b3486eee5a9b?proxyToken=notReal"

