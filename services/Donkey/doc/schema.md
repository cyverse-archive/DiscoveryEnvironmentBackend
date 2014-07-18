Here are the definitions of the data records used to communicate through the endpoints.

# Table of Contents

* [Permission](#permission)
* [AVU Record](#avu-record)
* [Filesystem Entry Record](#filesystem-entry-record)
    * [File Record](#file-record)
    * [Folder Record](#folder-record)
* [Permission Record](#permission-record)
* [User Details Record](#user-details-record)

# Permission
\* The `read` access level means the user can download a file or folder, read it, and read its
metadata. The `modify` access level gives the user `read` access level plus the ability to create,
modify and delete file or folder metadata. For a file, this access level also gives the user the
ability to modify the file. For a folder, this access level gives the ability to upload files and
folders into the folder. The `own` access level gives the user complete control over the file or
folder.

# AVU Record

Here are the fields that describe and attribute-value-unit (AVU) triple.

| Field     | Type   | Description |
| --------- | ------ | ----------- |
| attribute | string | the attribute's name |
| value     | string | the attribute's value |
| unit      | string | the attribute's unit, may be `null` |

**Example**

```json
{
    "attribute" : "length",
    "value"     : "1.8",
    "unit"      : "meter"
}
```

# Filesystem Entry Record

Here are the fields that describe a filesystem entry.

| Field           | Type   | Description |
| --------------- | ------ | ----------- |
| id              | string | the UUID of the entry |
| path            | string | the logical path to the entry |
| permission      | string | the aggregated [access level](#permission) of the client |
| userPermissions | array  | an array of [permission records](#permission-record) identifying the permissions users have on this entry |
| dateCreated     | number | the time when the file was created in milliseconds since the POSIX epoch |
| dateModified    | number | the time when the file was last modified in milliseconds since the POSIX epoch |
| label           | string | the logical name of the entry |

**Example**

```json
{
    "id"              : "deadbeef-dead-beef-dead-beefdeadbeef",
    "path"            : "/iplant/home/tedgin/an-entry",
    "permission"      : "own",
    "userPermissions" : [
        {
            "permission" : "own",
            "user"       : "tedgin#iplant"
        },
        {
            "permission" : "own",
            "user"       : "rodsadmin#iplant"
        }
    ],
    "dateCreated"     : 1410399966001,
    "dateModified"    : 1410399966001,
    "label"           : "an-entry"
}
```

## File Record

A file extends a [filesystem entry](#filesystem-entry-record). Here are the additional fields that
describe a file.

| Field    | Type   | Description |
| -------- | ------ | ----------- |
| creator  | string | the identity of the file's creator in name#zone format |
| fileSize | number | the size of the file in octets |
| fileType | string | the type of the file, `null` if unknown |
| metadata | array  | the metadata attached to this file, an array of [AVU records](#avu-record) |

**Example**

```json
{
    "id"              : ""deadbeef-dead-beef-dead-beefdeadbeef"",
    "path"            : "/iplant/home/tedgin/an.file",
    "permission"      : "own",
    "userPermissions" : [
        {
            "permission" : "own",
            "user"       : "tedgin#iplant"
        },
        {
            "permission" : "own",
            "user"       : "rodsadmin#iplant"
        }
    ],
    "creator"         : "tedgin#iplant",
    "dateCreated"     : 1410399966001,
    "dateModified"    : 1410399966001,
    "fileSize"        : 14016,
    "label"           : "a.file",
    "fileType"        : null,
    "metadata"        : [
        {
            "attribute" : "length",
            "value"     : "1.8",
            "unit"      : "meter",
        },
        {
            "attribute" : "color",
            "value"     : "red",
            "unit"      : null
        }
    ]
}
```

## Folder Record

A folder extends a [filesystem entry](#filesystem-entry-record). Here are the additional fields that
describe a folder.

| Field       | Type   | Description |
| ----------- | ------ | ----------- |
| creator     | string | the identity of the folder's creator in name#zone format |
| metadata    | array  | the metadata attached to this folder, an array of [AVU records](#avu-record) |

**Example**

```json
{
    "id"              : ""deadbeef-dead-beef-dead-beefdeadbeef"",
    "path"            : "/iplant/home/tedgin/a-folder",
    "permission"      : "own",
    "userPermissions" : [
        {
            "permission" : "own",
            "user"       : "tedgin#iplant"
        },
        {
            "permission" : "own",
            "user"       : "rodsadmin#iplant"
        }
    ],
    "creator"         : "tedgin#iplant",
    "dateCreated"     : 1410399966001,
    "dateModified"    : 1410399966001,
    "label"           : "a-folder",
    "metadata"        : [
        {
            "attribute" : "length",
            "value"     : "1.8",
            "unit"      : "meter",
        },
        {
            "attribute" : "color",
            "value"     : "red",
            "unit"      : null
        }
    ]
}
```

# Permission Record

Here are the fields that describe a permission.

| Field      | Type   | Description |
| ---------- | ------ | ----------- |
| permission | string | the [access level](#permission) |
| user       | string | the identity of the user having the given permission in name#zone format |


**Example**

```json
{
    "permission" : "own",
    "user"       : "tedgin#iplant"
}
```

# User Details Record

Here are the fields that describe a user's details.

| Field       | Type   | Description |
| ----------- | ------ | ----------- |
| username    | string | the authenticated name of the identified user |
| zone        | string | the iRODS zone where the user has been authenticated |
| email       | string | the user's email address |
| firstName   | string | the given name of the user |
| lastName    | string | the family name of the user |
| workspaceId | number | the internal identifier of the user's DE workspace |

**Example**

```json
{
    "username"    : "tedgin",
    "zone"        : "iplant",
    "email"       : "you.wish@keep-guessing.xxx",
    "firstName"   : "tony",
    "lastName"    : "tedgin",
    "workspaceId" : 4
}
```
