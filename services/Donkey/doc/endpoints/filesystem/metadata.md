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
        "path"   : "\/iplant\/home\/wregglej\/LICENSE.txt",
        "user"   :" wregglej"
    }

__Curl Command__:

    curl -d '{"add" : [{"attr" : "attr", "value" : "value", "unit" : "unit"}], "delete" : ["del1", "del2"]}' 'http://127.0.0.1:3000/secured/filesystem/metadata-batch?proxyToken=notReal&path=/iplant/home/johnw/LICENSE.txt'

Adding Batch Metadata to Multiple Paths
---------------------------------------
__URL Path__: /secured/filesystem/metadata-batch-add

__HTTP Method__: POST

__Error codes__: ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE, ERR_NOT_A_USER, ERR_TOO_MANY_PATHS, ERR_NOT_UNIQUE

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* force - Omitting this parameter will cause this endpoint to validate that none of the given paths
already have metadata set with any of the given attrs before adding the AVUs,
otherwise an ERR_NOT_UNIQUE error is returned.

__Request Body__:

```json
{
    "paths": [
        "/iplant/home/ipctest/folder-1",
        "/iplant/home/ipctest/folder-2",
        "/iplant/home/ipctest/file-1",
        "/iplant/home/ipctest/file-2"
    ],
    "avus": [
        {
            "unit": "",
            "value": "test value",
            "attr": "new-attr-1"
        },
        {
            "unit": "",
            "value": "test value",
            "attr": "new-attr-2"
        },
        {
            "unit": "",
            "value": "test value",
            "attr": "new-attr-3"
        }
    ]
}
```

Both "paths" and "avus" lists must be present and non-empty.

__Response__:

```json
{
    "paths": [
        "/iplant/home/ipctest/folder-1",
        "/iplant/home/ipctest/folder-2",
        "/iplant/home/ipctest/file-1",
        "/iplant/home/ipctest/file-2"
    ],
    "user":"ipctest"
}
```

__Curl Command__:

    curl -d '{"paths": ["/iplant/home/ipctest/folder-1","/iplant/home/ipctest/folder-2"], "avus": [{"attr": "attr", "value": "value", "unit": "unit"}]}' 'http://127.0.0.1:3000/secured/filesystem/metadata-batch-add?proxyToken=notReal&force=true'



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
        ]
    }

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/metadata/templates?proxyToken=notReal"


Viewing a Metadata Template
---------------------------
__URL Path__: /secured/filesystem/metadata/template/{template_id}

__HTTP Method__: GET

__Error Codes__: ERR_NOT_FOUND

__Response__:

```json
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
        {
            "id": "e7eb8aba-dc88-11e4-a4a9-2737bfa49b5e",
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "synonyms": [],
            "required": true,
            "type": "Enum",
            "values": [
                {
                    "is_default": false,
                    "value": "Yes",
                    "id": "e7ec2b0a-dc88-11e4-a4aa-1f3133b20123"
                },
                {
                    "is_default": false,
                    "value": "No",
                    "id": "e7ec83b6-dc88-11e4-a4ab-138d88f41d44"
                }
            ]
        },
        ...
    ],
    "id": "59bd3d26-34d5-4e75-99f5-840a20089caf",
    "name": "iDS Genome Sequences"
}
```

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/metadata/template/59bd3d26-34d5-4e75-99f5-840a20089caf?proxyToken=notReal"

Viewing a Metadata Attribute
----------------------------
__URL Path__: /secured/filesystem/metadata/template/attr/{attribute_id}

__HTTP Method__: GET

__Error Codes__: ERR_NOT_FOUND

__Response__:

```json
{
    "description": "project name",
    "id": "33e3e3d8-cd48-4572-8b16-89207b1609ec",
    "name": "project",
    "required": true,
    "synonyms": [],
    "type": "String"
}
```

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/metadata/template/attr/33e3e3d8-cd48-4572-8b16-89207b1609ec?proxyToken=notReal"

Adding Metadata Templates
---------------------------
__URL Path__: /admin/filesystem/metadata/templates

__HTTP Method__: POST

__Error Codes__: ERR_BAD_OR_MISSING_FIELD

__Request Body__:

```json
{
    "name": "iDS Genome Sequences",
    "attributes": [
        {
            "name": "project",
            "description": "project name",
            "required": true,
            "type": "String"
        },
        {
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "required": true,
            "type": "Enum",
            "values": [
                {
                    "value": "Yes",
                    "is_default": false
                },
                {
                    "value": "No",
                    "is_default": false
                }
            ]
        },
        ...
    ]
}
```

__Response__:

```json
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
        {
            "id": "e7eb8aba-dc88-11e4-a4a9-2737bfa49b5e",
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "synonyms": [],
            "required": true,
            "type": "Enum",
            "values": [
                {
                    "is_default": false,
                    "value": "Yes",
                    "id": "e7ec2b0a-dc88-11e4-a4aa-1f3133b20123"
                },
                {
                    "is_default": false,
                    "value": "No",
                    "id": "e7ec83b6-dc88-11e4-a4ab-138d88f41d44"
                }
            ]
        },
        ...
    ],
    "id": "59bd3d26-34d5-4e75-99f5-840a20089caf",
    "name": "iDS Genome Sequences"
}
```

__Curl Command__:

```json
curl -sd '
{
    "name": "iDS Genome Sequences",
    "attributes": [
        ...
    ]
}
' "http://127.0.0.1:3000/admin/filesystem/metadata/templates?proxyToken=notReal"
```

Updating Metadata Templates
---------------------------
__URL Path__: /admin/filesystem/metadata/templates/{template-id}

__HTTP Method__: POST

__Error Codes__: ERR_NOT_FOUND, ERR_BAD_OR_MISSING_FIELD

__Request Body__:

```json
{
    "name": "iDS Genome Sequences",
    "deleted": false,
    "attributes": [
        {
            "description": "project name",
            "id": "33e3e3d8-cd48-4572-8b16-89207b1609ec",
            "name": "project",
            "required": true,
            "synonyms": [],
            "type": "String"
        },
        {
            "id": "e7eb8aba-dc88-11e4-a4a9-2737bfa49b5e",
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "synonyms": [],
            "required": true,
            "type": "Enum",
            "values": [
                {
                    "is_default": false,
                    "value": "Yes",
                    "id": "e7ec2b0a-dc88-11e4-a4aa-1f3133b20123"
                },
                {
                    "is_default": false,
                    "value": "No",
                    "id": "e7ec83b6-dc88-11e4-a4ab-138d88f41d44"
                }
            ]
        },
        ...
    ]
}
```

__Response__:

```json
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
        {
            "id": "e7eb8aba-dc88-11e4-a4a9-2737bfa49b5e",
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "synonyms": [],
            "required": true,
            "type": "Enum",
            "values": [
                {
                    "is_default": false,
                    "value": "Yes",
                    "id": "e7ec2b0a-dc88-11e4-a4aa-1f3133b20123"
                },
                {
                    "is_default": false,
                    "value": "No",
                    "id": "e7ec83b6-dc88-11e4-a4ab-138d88f41d44"
                }
            ]
        },
        ...
    ],
    "id": "59bd3d26-34d5-4e75-99f5-840a20089caf",
    "name": "iDS Genome Sequences"
}
```

__Curl Command__:

```json
curl -sd '
{
    "name": "iDS Genome Sequences",
    "deleted": false,
    "attributes": [
        ...
    ]
}
' "http://127.0.0.1:3000/admin/filesystem/metadata/templates/59bd3d26-34d5-4e75-99f5-840a20089caf?proxyToken=notReal"
```

Viewing all Metadata Template AVUs on a File/Folder
-----------------------------------------------------
__URL Path__: /secured/filesystem/:data-id/template-avus

__HTTP Method__: GET

__Error Codes__: ERR_NOT_READABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

```json
{
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

Copying all Metadata Template AVUs from a File/Folder
-----------------------------------------------------
Copies all Metadata Template AVUs from the data item with the ID given in the URL to other data
items with the IDs sent in the request body.

__URL Path__: /secured/filesystem/:data-id/template-avus/copy

__HTTP Method__: POST

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_NOT_UNIQUE

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* force - Omitting this parameter, or setting its value to anything other than "true", will cause
this endpoint to validate that none of the given "destination_ids" already have Metadata Template
AVUs set with any of the attributes found in any of the Metadata Template AVUs associated with the
source "data-id", otherwise an ERR_NOT_UNIQUE error is returned.

__Request Body__:

```json
{
    "destination_ids": [
        "c5d42092-df89-11e3-bf8b-6abdce5a08d5",
        "..."
    ]
}
```

__Response__:

```json
{
    "user": "ipctest",
    "src": "/iplant/home/ipctest/folder-1",
    "paths": [
        "/iplant/home/ipctest/folder-2",
        "..."
    ]
}
```

__Curl Command__:

    curl -sd '{"destination_ids": ["c5d42092-df89-11e3-bf8b-6abdce5a08d5"]}' "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/copy?proxyToken=notReal&force=true"

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

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10?proxyToken=notReal"

Removing a Metadata Template AVU from a File/Folder
---------------------------------------------------------
__URL Path__: /secured/filesystem/:data-id/template-avus/:template-id/:avu-id

__HTTP Method__: DELETE

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10/b9315be3-5dd4-42bd-9b9f-b3486eee5a9b?proxyToken=notReal"

Copying all Metadata from a File/Folder
-----------------------------------------------------
Copies all IRODS AVUs visible to the client and Metadata Template AVUs from the data item with the
ID given in the URL to other data items with the IDs sent in the request body.

__URL Path__: /secured/filesystem/:data-id/metadata/copy

__HTTP Method__: POST

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_NOT_UNIQUE

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* force - Omitting this parameter, or setting its value to anything other than "true", will cause
this endpoint to validate that none of the given "destination_ids" already have Metadata Template
AVUs set with any of the attributes found in any of the Metadata Template AVUs associated with the
source "data-id", otherwise an ERR_NOT_UNIQUE error is returned.
IRODS allows duplicate attributes with different values on files and folders, so this endpoint will
also allow copies of IRODS AVUs to destination files/folders of duplicate attributes if the source
file/folder has a different value.

__Request Body__:

```json
{
    "destination_ids": [
        "c5d42092-df89-11e3-bf8b-6abdce5a08d5",
        "..."
    ]
}
```

__Response__:

```json
{
    "user": "ipctest",
    "src": "/iplant/home/ipctest/folder-1",
    "paths": [
        "/iplant/home/ipctest/folder-2",
        "..."
    ]
}
```

__Curl Command__:

    curl -sd '{"destination_ids": ["c5d42092-df89-11e3-bf8b-6abdce5a08d5"]}' "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/metadata/copy?proxyToken=notReal&force=true"

Exporting Metadata to a File
----------------------------

Secured Endpoint: POST /secured/filesystem/{data-id}/metadata/save

Delegates to data-info: POST /data/{data-id}/metadata/save

This endpoint is a passthrough to the data-info endpoint above.
Please see the data-info documentation for more information.
