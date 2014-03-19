# Table of Contents

* [Tool Request Services](#tool-request-services)
    * [Requesting Tool Installation](#requesting-tool-installation)
    * [Listing Tool Requests](#listing-tool-requests)
    * [Updating the Status of a Tool Request](#updating-the-status-of-a-tool-request)
    * [Obtaining Tool Request Details](#obtaining-tool-request-details)
    * [Listing Tool Request Status Codes](#listing-tool-request-status-codes)

# Tool Request Services

## Requesting Tool Installation

Secured Endpoint: PUT /secured/tool-request

This service submits a request for a tool to be installed so that it can be used
from within the Discovery Environment. The installation request and all status
updates related to the tool request will be tracked in the Discovery Environment
database. One possible request body format is:

```json
{
    "phone": "user-phone-number",
    "name": "tool-name",
    "description": "tool-description",
    "src_url": "link-to-tool-source",
    "documentation_url": "link-to-tool-documentation",
    "version": "tool-version",
    "attribution": "tool-attribution",
    "multithreaded": "multithreaded-flag",
    "test_data_file": "test-data-path",
    "cmd_line": "command-line-description",
    "additional_info": "optional-additional-info",
    "additional_data_file": "optional-additional-file",
    "architecture": "architecture flag"
}
```

All tool installation requests will look similar to this one, but some fields
may be replaced with others, depending on the nature of the request. A complete
description of the request body is included below with related fields organized
into groups. In cases where a multiple fields are in a required field group,
any one of the fields from that group may be specified.

<table border='1'>
    <thead>
        <tr>
            <th>Field Group</th>
            <th>Required</th>
            <th>Field Name</th>
            <th>Field Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Phone</td>
            <td>No</td>
            <td>phone</td>
            <td>The phone number of the user submitting the request.</td>
        </tr>
        <tr>
            <td>Tool Name</td>
            <td>Yes</td>
            <td>name</td>
            <td>The name of the tool being installed (should be the file name).</td>
        </tr>
        <tr>
            <td>Tool Description</td>
            <td>Yes</td>
            <td>description</td>
            <td>A brief description of the tool.</td>
        </tr>
        <tr>
            <td rowspan="2">Source Location</td>
            <td rowspan="2">Yes</td>
            <td>src_url</td>
            <td>A link that can be used to obtain the tool.</td>
        </tr>
        <tr>
            <td>src_upload_file</td>
            <td>The path to a file that has been uploaded into iRODS.</td>
        </tr>
        <tr>
            <td>Documentation Location</td>
            <td>Yes</td>
            <td>documentation_url</td>
            <td>A link to the tool documentation.</td>
        </tr>
        <tr>
            <td>Tool Version</td>
            <td>Yes</td>
            <td>version</td>
            <td>The tool's version string.</td>
        </tr>
        <tr>
            <td>Tool Attribution</td>
            <td>No</td>
            <td>attribution</td>
            <td>The people or organizations that produced the tool.</td>
        </tr>
        <tr>
            <td>Multithreaded Indicator</td>
            <td>No</td>
            <td>multithreaded</td>
            <td>
                A flag indicating whether or not the tool is multithreaded. This
                can be <code>Yes</code> to indicate that the user requesting the
                tool knows that it is multithreaded, <code>No</code> to indicate
                that the user knows that the tool is not multithreaded, or
                anything else to indicate that the user does not know whether or
                not the tool is multithreaded.
            </td>
        </tr>
        <tr>
            <td>Test Data Location</td>
            <td>Yes</td>
            <td>test_data_file</td>
            <td>
                The path to a test data file that has been uploaded to iRODS.
            </td>
        </tr>
        <tr>
            <td>Tool Usage Instructions</td>
            <td>Yes</td>
            <td>cmd_line</td>
            <td>Instructions for using the tool.</td>
        </tr>
        <tr>
            <td>Additional Tool Information</td>
            <td>No</td>
            <td>additional_info</td>
            <td>
                Any additional information that may be helpful during tool
                installation or validation.
            </td>
        </tr>
        <tr>
            <td>Additional Data File</td>
            <td>No</td>
            <td>additional_data_file</td>
            <td>
                Any additional data file that may be helpful during tool
                installation or validation.
            </td>
        </tr>
        <tr>
            <td>Tool Architecture</td>
            <td>Yes</td>
            <td>architecture</td>
            <td>
                One of the architecture names known to the DE. Currently, the
                valid values are `32-bit Generic` for a 32-bit executable that
                will run in the DE, `64-bit Generic` for a 64-bit executable
                that will run in the DE, `Others` for tools run in a virtual
                machine or interpreter, and `Don't know` if the user requesting
                the tool doesn't know what the architecture is.
            </td>
        </tr>
    </tbody>
</table>

The response body is a complete listing of the new tool request as returned by
the GET /tool-request service. Please see the description of that service for
more details.

Here's an example:

```
$ curl -sX PUT -d '
{
    "phone": "520-555-1212",
    "name": "jaguar",
    "description": "a really big cat",
    "src_url": "http://www.example.org/path/to/source.tar.gz",
    "documentation_url": "http://www.example.org/path/to/docs.html",
    "version": "1.0.0",
    "attribution": "An exemplary organization.",
    "multithreaded": "Yes",
    "test_data_file": "/path/to/test_file",
    "cmd_line": "jaguar some-file",
    "additional_info": "some additional info",
    "additional_data_file": "/path/to/additional_file",
    "architecture": "64-bit Generic"
}
' "http://by-tor:8888/secured/tool-request?user=nobody&email=nobody@iplantcollaborative.org" | python -mjson.tool
{
    "additional_data_file": "/path/to/additional_file",
    "additional_info": "some additional info",
    "architecture": "64-bit Generic",
    "attribution": "An exemplary organization.",
    "cmd_line": "jaguar some-file",
    "description": "a really big cat",
    "documentation_url": "http://www.example.org/path/to/docs.html",
    "history": [
        {
            "comments": "",
            "status": "Submitted",
            "status_date": "1364257498649",
            "updated_by": "nobody@iplantcollaborative.org"
        }
    ],
    "multithreaded": true,
    "name": "jaguar",
    "phone": "520-555-1212",
    "source_url": "http://www.example.org/path/to/source.tar.gz",
    "submitted_by": "nobody@iplantcollaborative.org",
    "success": true,
    "test_data_path": "/path/to/test_file",
    "uuid": "7C5ACB09-8675-4F04-B323-78431B801226",
    "version": "1.0.0"
}
```

## Listing Tool Requests

Unsecured Endpoint: GET /tool-requests

Secured Endpoint: GET /secured/tool-requests

These endpoint list high level details about tool requests that have been
submitted. The unsecured endpoint is intended for use by administrators to track
tool requests for all users. The secured endpoint is intended for use by users
to track their own tool requests. The number of results returned and the order
of the results can be controlled by query-string parameters:

<table>
    <thead>
        <tr>
            <th>Parameter Name(s)</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>sortfield</td>
            <td rowspan="2">
                The field to use when sorting the tool installation requests.
                This can be any field that appears in each tool request in the
                response body.
            </td>
        </tr>
        <tr>
            <td>sortField</td>
        </tr>
        <tr>
            <td>sortdir</td>
            <td rowspan="2">
                The sort order to use in the response list. This can be either
                `asc` for ascending or `desc` for descending. The values of this
                field are case-insensitive.
            </td>
        </tr>
        <tr>
            <td>sortDir</td>
        </tr>
        <tr>
            <td>limit</td>
            <td>The maximum number of results to return.</td>
        </tr>
        <tr>
            <td>offset</td>
            <td>The index of the first result to return.</td>
        </tr>
        <tr>
            <td>status</td>
            <td>
                The name of a status code to include in the results. The
                name of the status code is case sensitive. If the status code
                isn't already defined, it will be added to the database.
            </td>
        </tr>
    </tbody>
</table>

The response body is in the following format:

```json
{
    "success": true,
    "tool_requests": [
        {
            "date_submitted": "timestamp",
            "date_updated": "timestamp",
            "name": "tool-name",
            "requested_by": "username",
            "status": "tool-request-status",
            "updated_by": "username",
            "uuid": "tool-request-id",
            "version": "tool-version"
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/tool-requests?user=nobody&limit=1" | python -mjson.tool
{
    "success": true,
    "tool_requests": [
        {
            "date_submitted": "1363047831085",
            "date_updated": "1363047831085",
            "name": "jaguar",
            "requested_by": "nobody@iplantcollaborative.org",
            "status": "Submitted",
            "updated_by": "nobody@iplantcollaborative.org",
            "uuid": "D38A108F-0626-481B-A4C9-356462916642",
            "version": "1.0.0"
        }
    ]
}
```

## Updating the Status of a Tool Request

Unsecured Endpoint: POST /tool-request

This endpoint is used by Discovery Environment administrators to update the
status of a tool request. The request body is in the following format:

```json
{
    "uuid": "tool-request-uuid",
    "status": "new-status-code",
    "username": "de-administrator-username",
    "comments": "administrator-comments"
}
```

The status code is case-sensitive, and if the status code isn't defined in the
database already then it will be added to the list of known status codes.

The respose body is in the same format as the GET /tool-request service. Please
see the documentation for that service for more information.

Here's an example:

```
$ curl -sd '
{
    "uuid": "7C5ACB09-8675-4F04-B323-78431B801226",
    "status": "Evaluation",
    "username": "someadmin",
    "comments": "About to do the evaluation."
}
' http://by-tor:8888/tool-request | python -mjson.tool
{
    "additional_data_file": "/path/to/additional_file",
    "additional_info": "some additional info",
    "architecture": "64-bit Generic",
    "attribution": "An exemplary organization.",
    "cmd_line": "jaguar some-file",
    "description": "a really big cat",
    "documentation_url": "http://www.example.org/path/to/docs.html",
    "history": [
        {
            "comments": "",
            "status": "Submitted",
            "status_date": "1364257498649",
            "updated_by": "nobody@iplantcollaborative.org"
        },
        {
            "comments": "About to do the evaluation.",
            "status": "Evaluation",
            "status_date": "1364328278490",
            "updated_by": "someadmin@iplantcollaborative.org"
        }
    ],
    "multithreaded": true,
    "name": "jaguar",
    "phone": "520-555-1212",
    "source_url": "http://www.example.org/path/to/source.tar.gz",
    "submitted_by": "nobody@iplantcollaborative.org",
    "success": true,
    "test_data_path": "/path/to/test_file",
    "uuid": "7C5ACB09-8675-4F04-B323-78431B801226",
    "version": "1.0.0"
}
```

## Obtaining Tool Request Details

Unsecured Endpoint: GET /tool-request/{uuid}

This service obtains detailed information about a tool request. This is the
service that the DE support team uses to obtain the request details. The
response body is in the following format:

```json
{
    "additional_data_file": "some-irods-path",
    "additional_info": "some-additional-info",
    "architecture": "tool-architecture-name",
    "attribution": "tool-attribution",
    "cmd_line": "command-line-description",
    "description": "tool-description",
    "documentation_url": "link-to-tool-documentation",
    "history": [
        {
            "comments": "status-change-comments",
            "status": "status-code",
            "status_date": "milliseconds-since-epoch",
            "updated_by": "username"
        },
        ...
    ],
    "multithreaded": "multithreaded-flag",
    "name": "tool-name",
    "phone": "user-phone-number",
    "source_url": "link-or-path-to-tool-source",
    "submitted_by": "username",
    "success": true,
    "test_data_path": "path-to-test-data",
    "uuid": "tool-request-uuid",
    "version": "tool-version"
}
```

Here's an example:

```
$ curl -s http://by-tor:8888/tool-request/7C5ACB09-8675-4F04-B323-78431B801226 | python -mjson.tool
{
    "additional_data_file": "/path/to/additional_file",
    "additional_info": "some additional info",
    "architecture": "64-bit Generic",
    "attribution": "An exemplary organization.",
    "cmd_line": "jaguar some-file",
    "description": "a really big cat",
    "documentation_url": "http://www.example.org/path/to/docs.html",
    "history": [
        {
            "comments": "",
            "status": "Submitted",
            "status_date": "1364257498649",
            "updated_by": "nobody@iplantcollaborative.org"
        },
        {
            "comments": "About to do the evaluation.",
            "status": "Evaluation",
            "status_date": "1364328278490",
            "updated_by": "someadmin@iplantcollaborative.org"
        }
    ],
    "multithreaded": true,
    "name": "jaguar",
    "phone": "520-555-1212",
    "source_url": "http://www.example.org/path/to/source.tar.gz",
    "submitted_by": "nobody@iplantcollaborative.org",
    "success": true,
    "test_data_path": "/path/to/test_file",
    "uuid": "7C5ACB09-8675-4F04-B323-78431B801226",
    "version": "1.0.0"
}
```

## Listing Tool Request Status Codes

Unsecured Endpoint: GET /tool-request-status-codes

Tool request status codes are largely arbitrary, but once they've been used
once, They're stored in the database so that they can be reused easily. This
endpoint allows the caller to list the known status codes. The response body
looks like this:

```json
{
    "statusCodes": [
       {
           "description": "status code description",
           "id": "status code ID",
           "name": "status code Name"
       },
       ...
    ],
    "success": true
}
```

In the default case, the endpoint just lists all of the status codes:

```
$ curl -s http://by-tor:8888/tool-request-status-codes | python -mjson.tool
{
    "status_codes": [
        {
            "description": "The tool has been installed successfully.",
            "id": "5ed94200-7565-45d8-b576-d7ff839e9993",
            "name": "Completion"
        },
        ...
    ],
    "success": true
}
```

If the `filter` query parameter is used then only the status codes that contain
the string passed in the query parameter will be listed. This is a
case-insensitive search:

```
$ curl -s http://localhost:3000/tool-request-status-codes?filter=some | python -mjson.tool
{
    "status_codes": [
        {
            "description": "Some Other Status",
            "id": "0538ee26-f728-4d39-92e8-31c3ca7900be",
            "name": "Some Other Status"
        },
        {
            "description": "Some Status",
            "id": "4fb01a4c-67d3-4f18-ba52-16e4627ba799",
            "name": "Some Status"
        }
    ],
    "success": true
}
```
