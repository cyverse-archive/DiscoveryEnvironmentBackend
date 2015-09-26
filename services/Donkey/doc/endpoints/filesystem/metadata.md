Metadata
---------------------------

The following commands allow the caller to set and get attributes on files and directories in iRODS. iRODS attributes take the form of Attribute Value Unit triples associated with directories and files. iRODS AVUs are only unique on the full triple, so duplicate AVUs may exist. DE tools do not, in general, use the unit field.


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
The `secured` and `admin` endpoints return the same listing, except the `admin` endpoint also
includes Metadata Templates that have been marked as deleted.

__URL Path__: /secured/filesystem/metadata/templates

__URL Path__: /admin/filesystem/metadata/templates

__HTTP Method__: GET

__Response__:

```json
{
    "metadata_templates": [
        {
            "id": "59bd3d26-34d5-4e75-99f5-840a20089caf",
            "name": "iDS Genome Sequences",
            "deleted": false,
            "created_by": "<public>",
            "created_on": "2015-04-24T19:23:47Z",
            "modified_by": "<public>",
            "modified_on": "2015-04-24T19:23:47Z"
        }
    ]
}
```

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
            "modified_on": "2015-04-09T00:22:44Z",
            "modified_by": "<public>",
            "created_on": "2015-04-09T00:22:44Z",
            "created_by": "<public>",
            "required": true,
            "synonyms": [],
            "type": "String"
        },
        {
            "id": "e7eb8aba-dc88-11e4-a4a9-2737bfa49b5e",
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "modified_on": "2015-04-09T00:22:44Z",
            "modified_by": "<public>",
            "created_on": "2015-04-09T00:22:44Z",
            "created_by": "<public>",
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
    "modified_on": "2015-04-09T00:22:44Z",
    "modified_by": "<public>",
    "created_on": "2015-04-09T00:22:44Z",
    "created_by": "<public>",
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
            "modified_on": "2015-04-09T00:38:31Z",
            "modified_by": "<public>",
            "created_on": "2015-04-09T00:22:44Z",
            "created_by": "<public>",
            "required": true,
            "synonyms": [],
            "type": "String"
        },
        {
            "id": "e7eb8aba-dc88-11e4-a4a9-2737bfa49b5e",
            "name": "medical_relevance",
            "description": "Indicate whether BioProject is of medical relevance",
            "modified_on": "2015-04-09T00:38:31Z",
            "modified_by": "<public>",
            "created_on": "2015-04-09T00:22:44Z",
            "created_by": "<public>",
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
    "modified_on": "2015-04-09T00:38:31Z",
    "modified_by": "<public>",
    "created_on": "2015-04-09T00:22:44Z",
    "created_by": "<public>",
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

Marking a Metadata Template as Deleted
----------------------------------------------------------
__URL Path__: /admin/filesystem/metadata/templates/{template-id}

__HTTP Method__: DELETE

__Error Codes__: ERR_DOES_NOT_EXIST

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/admin/filesystem/metadata/templates/59bd3d26-34d5-4e75-99f5-840a20089caf?proxyToken=notReal"

Adding Batch Metadata to Multiple Paths from a CSV File
-------------------------------------------------------
These endpoints will parse a CSV/TSV file where the first column is absolute or relative paths to
files in the data store, the remaining columns are metadata, attributes are listed in the first row,
and filenames and attribute values are listed in the remaining rows.
If a `template-id` parameter is provided, then any parsed AVUs with attributes that match the given
template's attributes will be added as template AVUs, otherwise all other AVUs will be added as
IRODS metadata AVUs.

__URL Path__: /secured/filesystem/metadata/csv-form-parser

__URL Path__: /secured/filesystem/metadata/csv-parser

__HTTP Method__: POST

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_NOT_UNIQUE

__Request Query Parameters__:

Parameter | Required | Description
----------|----------|------------
proxyToken | Yes | A valid CAS ticket.
dest | Yes | The folder path to look under for files listed in the CSV file.
src | Yes/No | Required only for the `/secured/filesystem/metadata/csv-parser` endpoint. Path to the CSV source file in IRODS.
force | No | If omitted or set to `false`, then existing IRODS AVUs will be checked for attributes matching those parsed from the CSV file. If a match is found, then an `ERR_NOT_UNIQUE` is returned and metadata is not saved.
template-id | No | The UUID of the Metadata Template with which to associate the parsed metadata.
separator | No | URL encoded separator character to use for parsing the CSV/TSV file. Comma (%2C) by default.

__Request File Format__:

filename | template_item | template_postal_code | template_department | template_institution | test-attr-1 | test-attr-2 | test-attr-3
---------|---|---|---|---|---|---|---
library1/fake.1.fastq.gz | fake-1 | 85719 | BIO5 | iPlant | test-val-1 | test-val-2 | test-val-3
/iplant/home/ipcuser/folder_2/library2/fake.2.fastq.gz | fake-2 | 85719 | BIO5 | iPlant | test-val-1 | test-val-2 | test-val-3
library1 | lib-1 | 85719 | BIO5 | iPlant | test-val-1 | test-val-2 | test-val-3

__Response__:

```json
{
    "metadata": [
        {
            "path": "/iplant/home/ipcuser/folder_1/library1/fake.1.fastq.gz",
            "template-avus": [
                {
                    "attr": "template_item",
                    "value": "fake-1",
                    "unit": ""
                },
                {
                    "attr": "template_postal_code",
                    "value": "85719",
                    "unit": ""
                },
                {
                    "attr": "template_department",
                    "value": "BIO5",
                    "unit": ""
                },
                {
                    "attr": "template_institution",
                    "value": "iPlant",
                    "unit": ""
                }
            ],
            "irods-avus": [
                {
                    "attr": "test-attr-1",
                    "value": "test-val-1",
                    "unit": ""
                },
                {
                    "attr": "test-attr-2",
                    "value": "test-val-2",
                    "unit": ""
                },
                {
                    "attr": "test-attr-3",
                    "value": "test-val-3",
                    "unit": ""
                }
            ]
        },
        {
            "path": "/iplant/home/ipcuser/folder_2/library2/fake.2.fastq.gz",
            "template-avus": [
                {
                    "attr": "template_item",
                    "value": "fake-2",
                    "unit": ""
                },
                {
                    "attr": "template_postal_code",
                    "value": "85719",
                    "unit": ""
                },
                {
                    "attr": "template_department",
                    "value": "BIO5",
                    "unit": ""
                },
                {
                    "attr": "template_institution",
                    "value": "iPlant",
                    "unit": ""
                }
            ],
            "irods-avus": [
                {
                    "attr": "test-attr-1",
                    "value": "test-val-1",
                    "unit": ""
                },
                {
                    "attr": "test-attr-2",
                    "value": "test-val-2",
                    "unit": ""
                },
                {
                    "attr": "test-attr-3",
                    "value": "test-val-3",
                    "unit": ""
                }
            ]
        },
        {
            "path": "/iplant/home/ipcuser/folder_1/library1",
            "template-avus": [
                {
                    "attr": "template_item",
                    "value": "lib-1",
                    "unit": ""
                },
                {
                    "attr": "template_postal_code",
                    "value": "85719",
                    "unit": ""
                },
                {
                    "attr": "template_department",
                    "value": "BIO5",
                    "unit": ""
                },
                {
                    "attr": "template_institution",
                    "value": "iPlant",
                    "unit": ""
                }
            ],
            "irods-avus": [
                {
                    "attr": "test-attr-1",
                    "value": "test-val-1",
                    "unit": ""
                },
                {
                    "attr": "test-attr-2",
                    "value": "test-val-2",
                    "unit": ""
                },
                {
                    "attr": "test-attr-3",
                    "value": "test-val-3",
                    "unit": ""
                }
            ]
        }
    ]
}
```

__Curl Command__:

    curl -s -X POST "http://localhost:3000/secured/filesystem/metadata/csv-parser?proxyToken=ipcuser&template-id=e7e19316-dc88-11e4-a49a-77c52ae8901a&dest=/iplant/home/ipcuser/folder_1&src=/iplant/home/ipcuser/metadata.csv"

    curl -s -F file=@metadata.tsv "http://localhost:3000/secured/filesystem/metadata/csv-form-parser?proxyToken=ipcuser&template-id=e7e19316-dc88-11e4-a49a-77c52ae8901a&dest=/iplant/home/ipcuser/folder_1&separator=%09"

Viewing all Metadata Template AVUs on a File/Folder
-----------------------------------------------------
__URL Path__: /secured/filesystem/{data-id}/template-avus

__HTTP Method__: GET

__Delegates to metadata__: `GET /filesystem/entry/{data-id}/avus`

__Error Codes__: ERR_NOT_READABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus?proxyToken=notReal"

Viewing a Metadata Template's AVUs on a File/Folder
---------------------------------------------------------
__URL Path__: /secured/filesystem/{data-id}/template-avus/{template-id}

__HTTP Method__: GET

__Delegates to metadata__: `GET /filesystem/entry/{data-id}/avus/{template-id}`

__Error Codes__: ERR_NOT_READABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Response__:

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

__Curl Command__:

    curl -s "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10?proxyToken=notReal"

Copying all Metadata Template AVUs from a File/Folder
-----------------------------------------------------
Copies all Metadata Template AVUs from the data item with the ID given in the URL to other data
items with the IDs sent in the request body.

__URL Path__: /secured/filesystem/{data-id}/template-avus/copy

__HTTP Method__: POST

__Delegates to metadata__: `POST /filesystem/entry/{data-id}/avus/copy`

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_NOT_UNIQUE

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* force - This endpoint forwards requests to the corresponding metadata service endpoint.
          Please see the metadata documentation for more information.

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
Saves Metadata AVUs on the given data item, associating them with the given Metadata Template.

__URL Path__: /secured/filesystem/{data-id}/template-avus/{template-id}

__HTTP Method__: POST

__Delegates to metadata__: `POST /filesystem/entry/{data-id}/avus/{template-id}`

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD

__Request Body__:

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

__Response__:

This endpoint forwards requests to the corresponding metadata service endpoint.
Please see the metadata documentation for more information.

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
__URL Path__: /secured/filesystem/{data-id}/template-avus/{template-id}

__HTTP Method__: DELETE

__Delegates to metadata__: `DELETE /filesystem/entry/{data-id}/avus/{template-id}`

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10?proxyToken=notReal"

Removing a Metadata Template AVU from a File/Folder
---------------------------------------------------------
__URL Path__: /secured/filesystem/{data-id}/template-avus/{template-id}/{avu-id}

__HTTP Method__: DELETE

__Delegates to metadata__: `DELETE /filesystem/entry/{data-id}/avus/{template-id}/{avu-id}`

__Error Codes__: ERR_NOT_READABLE, ERR_NOT_WRITEABLE, ERR_DOES_NOT_EXIST, ERR_NOT_A_USER

__Curl Command__:

    curl -X DELETE "http://127.0.0.1:3000/secured/filesystem/cc20cbf8-df89-11e3-bf8b-6abdce5a08d5/template-avus/40ac191f-bb36-4f4e-85fb-8b50abec8e10/b9315be3-5dd4-42bd-9b9f-b3486eee5a9b?proxyToken=notReal"

Copying all Metadata from a File/Folder
-----------------------------------------------------
Copies all IRODS AVUs visible to the client and Metadata Template AVUs from the data item with the
ID given in the URL to other data items with the IDs sent in the request body.

__URL Path__: /secured/filesystem/{data-id}/metadata/copy

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
