# Table of Contents

* [Application Metadata Endpoints](#application-metadata-endpoints)
    * [Listing App Elements](#listing-app-elements)
    * [Searching for Deployed Components](#searching-for-deployed-components)
    * [Listing App Identifiers](#listing-app-identifiers)
    * [Deleting Categories](#deleting-categories)
    * [Valiating Analyses for Pipelines](#valiating-analyses-for-pipelines)
    * [Listing Data Objects in an Analysis](#listing-data-objects-in-an-analysis)
    * [Categorizing Apps](#categorizing-apps)
    * [Listing Analysis Categorizations](#listing-analysis-categorizations)
    * [Determining if an Analysis Can be Exported](#determining-if-an-analysis-can-be-exported)
    * [Adding Analyses to Analysis Groups](#adding-analyses-to-analysis-groups)
    * [Getting Analyses in the JSON Format Required by the DE](#getting-analyses-in-the-json-format-required-by-the-de)
    * [Getting App Details](#getting-app-details)
    * [Listing App Groups](#listing-app-groups)
    * [Listing Individual Analyses](#listing-individual-analyses)
    * [Exporting a Template](#exporting-a-template)
    * [Exporting an Analysis](#exporting-an-analysis)
    * [Exporting Selected Deployed Components](#exporting-selected-deployed-components)
    * [Logically Deleting Apps](#logically-deleting-apps)
    * [Previewing Templates](#previewing-templates)
    * [Previewing Analyses](#previewing-analyses)
    * [Updating an Existing Template](#updating-an-existing-template)
    * [Updating an Analysis](#updating-an-analysis)
    * [Forcing an Analysis to be Updated](#forcing-an-analysis-to-be-updated)
    * [Updating App Labels](#updating-app-labels)
    * [Importing a Template](#importing-a-template)
    * [Importing an Analysis](#importing-an-analysis)
    * [Importing Deployed Components](#importing-deployed-components)
    * [Updating Top-Level Analysis Information](#updating-top-level-analysis-information)
    * [Rating Apps](#rating-apps)
    * [Deleting App Ratings](#deleting-app-ratings)
    * [Searching for Apps](#searching-for-apps)
    * [Listing Apps in an App Group](#listing-apps-in-an-app-group)
    * [Listing Deployed Components in an Analysis](#listing-deployed-components-in-an-analysis)
    * [Updating the Favorite Analyses List](#updating-the-favorite-analyses-list)
    * [Making a Copy of an Analysis Available for Editing in Tito](#making-a-copy-of-an-analysis-available-for-editing-in-tito)
    * [Submitting an Analysis for Public Use](#submitting-an-analysis-for-public-use)
    * [Determining if an Analysis Can be Made Public](#determining-if-an-analysis-can-be-made-public)
    * [Making a Pipeline Available for Editing](#making-a-pipeline-available-for-editing)
    * [Making a Copy of a Pipeline Available for Editing](#making-a-copy-of-a-pipeline-available-for-editing)
    * [Requesting Installation of a Tool](#requesting-installation-of-a-tool)
    * [Updating a Tool Installation Request](#updating-a-tool-installation-request)
    * [Listing Tool Installation Requests](#listing-tool-installation-requests)
    * [Listing Tool Installation Request Details](#listing-tool-installation-request-details)
    * [Listing Tool Request Status Codes](#listing-tool-request-status-codes)

# Application Metadata Endpoints

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](data-info-v-metadactyl.md) for more
information.

## Listing App Elements

Secured Endpoint: GET /apps/elements/{element-type}

Delegates to metadactyl: GET /apps/elements/{element-type}

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Searching for Deployed Components

Unsecured Endpoint: GET /search-deployed-components/{search-term}

Delegates to metadactyl: GET /search-deployed-components{search-term}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing App Identifiers

Secured Endpoint: GET /apps/ids

Delegates to metadactyl: GET /apps/ids

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Deleting Categories

Unsecured Endpoint: POST /delete-categories

Delegates to metadactyl: POST /delete-categories

This endpoint is a passthrough to the metactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Valiating Analyses for Pipelines

Unsecured Endpoint: GET /validate-analysis-for-pipelines/{analysis-id}

Delegates to metadactyl: GET /validate-analysis-for-pipelines/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Listing Data Objects in an Analysis

Secured Endpoint: GET /secured/apps/{app-id}/data-objects

This service obtains the lists of inputs and outputs for an app. For apps that
run within the DE itself, this service delegates to the metadactyl endpoint,
`GET /apps/{app-id}/data-objects`. For other apps, the response is assembled
within data-info from information received from remote services. Here's an
example:

```
$ curl -s "http://by-tor:8888/secured/apps/wc-osg-1.00u1/data-objects?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "id": "wc-osg-1.00u1",
    "inputs": [
        {
            "arguments": [],
            "data_object": {
                "cmdSwitch": "query1",
                "description": "",
                "file_info_type": "File",
                "format": "Unspecified",
                "id": "query1",
                "multiplicity": "One",
                "name": "File to count words in: ",
                "order": 1,
                "required": false,
                "retain": false
            },
            "defaultValue": "",
            "description": "",
            "id": "query1",
            "isVisible": true,
            "label": "File to count words in: ",
            "name": "query1",
            "order": 0,
            "required": false,
            "type": "FileInput",
            "validators": []
        }
    ],
    "name": "wc-osg [wc-osg-1.00u1]",
    "outputs": [
        {
            "arguments": [],
            "data_object": {
                "cmdSwitch": "outputWC",
                "description": "Results of WC",
                "file_info_type": "File",
                "format": "Unspecified",
                "id": "outputWC",
                "multiplicity": "One",
                "name": "Text file",
                "order": 1,
                "required": false,
                "retain": false
            },
            "defaultValue": "wc_out.txt",
            "description": "Results of WC",
            "id": "outputWC",
            "isVisible": false,
            "label": "Text file",
            "name": "outputWC",
            "order": 0,
            "required": false,
            "type": "Output",
            "validators": []
        }
    ],
    "success": true
}

```

## Categorizing Apps

Secured Endpoint: POST /admin/apps

Delegates to metadactyl: POST /admin/apps

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Listing Analysis Categorizations

Unsecured Endpoint: GET /get-analysis-categories/{category-set}

Delegates to metadactyl: GET /get-analysis-categories/{category-set}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Determining if an Analysis Can be Exported

Unsecured Endpoint: POST /can-export-analysis

Delegates to metadactyl: POST /can-export-analysis

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Adding Analyses to Analysis Groups

Unsecured Endpoint: POST /add-analysis-to-group

Delegates to metadactyl: POST /add-analysis-to-group

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Getting Analyses in the JSON Format Required by the DE

Unsecured Endpoint: GET /get-analysis/{analysis-id}

Delegates to metadactyl: GET /get-analysis/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Getting App Details

Secured Endpoint: GET /apps/{app-id}/details

This service is used by the DE to obtain high-level details about a single
analysis.

For DE apps, this service delegates the call to the metadactyl endpoint, `/apps/{app-id}/details`.
Please see the metadactyl documentation for more information its response format.

For Agave apps, this service retrieves the information it needs to format the response from Agave.
Here's an example of an Agave app listing:

```
 curl -s "http://gargery:31325/apps/wc-1.00u1/details?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "components": [
        {
            "attribution": "",
            "description": "Count words in a file",
            "id": "wc-1.00u1",
            "location": "/ipcservices/applications",
            "name": "wc-1.00u1.zip",
            "type": "HPC",
            "version": "1.00"
        }
    ],
    "description": "Count words in a file",
    "edited_date": "1383351103584",
    "groups": [
        {
            "id": "HPC",
            "name": "High-Performance Computing"
        }
    ],
    "id": "wc-1.00u1",
    "label": "Word Count",
    "name": "Word Count",
    "published_date": "1383351103584",
    "refrences": [],
    "success": true,
    "suggested_groups": [
        {
            "id": "HPC",
            "name": "High-Performance Computing"
        }
    ],
    "tito": "wc-1.00u1"
}
```

## Listing App Groups

Secured Endpoint: GET /apps/categories

Delegates to metadactyl: GET /apps/categories

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Listing Individual Analyses

Unsecured Endpoint: GET /list-analysis/{analysis-id}

Delegates to metadactyl: GET /list-analysis/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Exporting a Template

Unsecured Endpoint: GET /export-template/{template-id}

Delegates to metadactyl: GET /export-template/{template-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Exporting an Analysis

Unsecured Endpoint: GET /export-workflow/{analysis-id}

Delegates to metadactyl: GET /export-workflow/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Exporting Selected Deployed Components

Unsecured Endpoint: POST /export-deployed-components

Delegates to metadactyl: POST /export-deployed-components

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Logically Deleting Apps

Secured Endpoint: DELETE /apps/{app-id}
Delegates to metadactyl: DELETE /apps/{app-id}

Secured Endpoint: POST /apps/shredder
Delegates to metadactyl: POST /apps/shredder

These endpoints are passthroughs to their corresponding metadactyl endpoint.
Please see the metadactyl documentation for more information.

## Previewing Templates

Unsecured Endpoint: POST /preview-template

Delegates to metadactyl: POST /preview-template

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Previewing Analyses

Unsecured Endpoint: POST /preview-workflow

Delegates to metadactyl: POST /preview-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating an Existing Template

Unsecured Endpoint: POST /update-template

Delegates to metadactyl: POST /update-template

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating an Analysis

Unsecured Endpoint: POST /update-workflow

Delegates to metadactyl: POST /update-workflow

Secured Endpoint: POST /secured/update-workflow

Delegates to metadactyl: POST /secured/update-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Forcing an Analysis to be Updated

Unsecured Endpoint: POST /force-update-workflow

Delegates to metadactyl: POST /force-update-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating App Labels

Secured Endpoint: PATCH /apps/{app-id}

Delegates to metadactyl: PATCH /apps/{app-id}

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Importing a Template

Unsecured Endpoint: POST /import-template

Delegates to metadactyl: POST /import-template

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Importing an Analysis

Unsecured Endpoint: POST /import-workflow

Delegates to metadactyl: POST /import-workflow

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Importing Deployed Components

Unsecured Endpoint: POST /import-tools

Delegates to metadactyl: POST /import-tools

This service is an extension of the /import-workflow endpoint that also sends a
notification for every deployed component that is imported provided that a
username and e-mail address is provided for the notification. The request body
should be in the following format:

```json
{
    "components": [
        {
            "name": "component-name",
            "location": "component-location",
            "implementation": {
                "implementor_email": "e-mail-address-of-implementor",
                "implementor": "name-of-implementor",
                "test": {
                    "params": [
                        "param-1",
                        "param-2",
                        "param-n"
                    ],
                    "input_files": [
                        "input-file-1",
                        "input-file-2",
                        "input-file-n"
                    ],
                    "output_files": [
                        "output-file-1",
                        "output-file-2",
                        "output-file-n"
                    ]
                }
            },
            "type": "deployed-component-type",
            "description": "deployed-component-description",
            "version": "deployed-component-version",
            "attribution": "deployed-component-attribution",
            "user": "username-for-notification",
            "email": "e-mail-address-for-notification"
        }
    ]
}
```

Note that this format is identical to the one used by the /import-workflow
endpoint except that the `user` and `email` fields have been added to allow
notifications to be generated automatically. If either of these fields is
missing or empty, a notification will not be sent even if the deployed component
is imported successfully.

The response body for this service contains a success flag along with a brief
description of the reason for the failure if the deployed components can't be
imported.

Here's an example of a successful import:

```
$ curl -sd '
{
    "components": [
        {
            "name": "foo",
            "location": "/usr/local/bin",
            "implementation": {
                "implementor_email": "nobody@iplantcollaborative.org",
                "implementor": "Nobody",
                "test": {
                    "params": [],
                    "input_files": [],
                    "output_files": []
                }
            },
            "type": "executable",
            "description": "the foo is in the bar",
            "version": "1.2.3",
            "attribution": "the foo needs no attribution",
            "user": "nobody",
            "email": "nobody@iplantcollaborative.org"
        }
    ]
}
' http://by-tor:8888/import-tools | python -mjson.tool
{
    "success": true
}
```

Here's an example of an unsuccessful import:

```
$ curl -sd '
{
    "components": [
        {
            "name": "foo",
            "location": "/usr/local/bin",
            "implementation": {
                "implementor_email": "nobody@iplantcollaborative.org",
                "implementor": "Nobody"
            },
            "type": "executable",
            "description": "the foo is in the bar",
            "version": "1.2.3",
            "attribution": "the foo needs no attribution",
            "user": "nobody",
            "email": "nobody@iplantcollaborative.org"
        }
    ]
}
' http://by-tor:8888/import-tools | python -mjson.tool
{
    "reason": "org.json.JSONException: JSONObject[\"test\"] not found.",
    "success": false
}
```

Though it is possible to import analyses using this endpoint, this practice is
not recommended because it can cause spurious notifications to be sent.

## Updating Top-Level Analysis Information

Unsecured Endpoint: POST /update-analysis

Delegates to metadactyl: POST /update-analysis

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Rating Apps

Secured Endpoint: POST /apps/{app-id}/rating

Delegates to metadactyl: POST /apps/{app-id}/rating

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Deleting App Ratings

Secured Endpoint: DELETE /apps/{app-id}/rating

Delegates to metadactyl: DELETE /apps/{app-id}/rating

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Searching for Apps

Secured Endpoint: GET /apps?search={term}

Delegates to metadactyl: GET /apps?search={term}

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Listing Apps in an App Group

Secured Endpoint: GET /apps/categories/{group-id}

Delegates to metadactyl: GET /apps/categories/{group-id}

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Listing Deployed Components in an Analysis

Secured Endpoint: GET /secured/get-components-in-analysis/{analysis-id}

Delegates to metadactyl: GET /secured/get-components-in-analysis/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Updating the Favorite Analyses List

Secured Endpoint: POST /secured/update-favorites

Delegates to metadactyl: POST /secured/update-favorites

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Making a Copy of an Analysis Available for Editing in Tito

Secured Endpoint: GET /secured/copy-template/{analysis-id}

Delegates to metadactyl: GET /secured/copy-template/{analysis-id}

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Submitting an Analysis for Public Use

Secured Endpoint: POST /secured/make-analysis-public

Delegates to metadactyl: POST /secured/make-analysis-public

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Determining if an App Can be Made Public

Secured Endpoint: GET /apps/{app-id}/is-publishable

Delegates to metadactyl: GET /apps/{app-id}/is-publishable

This endpoint is a passthrough to the metadactyl endpoint using the path above.
Please see the metadactyl documentation for more information.

## Making a Pipeline Available for Editing

Secured Endpoint: GET /apps/{app-id}/pipeline-ui

Delegates to metadactyl: GET /apps/{app-id}/pipeline-ui

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Making a Copy of a Pipeline Available for Editing

Secured Endpoint: POST /apps/{app-id}/copy-pipeline

Delegates to metadactyl: POST /apps/{app-id}/copy-pipeline

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Requesting Installation of a Tool

Secured Endpoint: POST /tool-requests

Delegates to metadactyl: POST /tool-requests

This service is primarily a passthrough to the metadactyl endpoint using the
same path. The only difference is that this endpoint also sends a message to the
tool request email address and generates a notification for the new tool request
indicating that the tool request was successfully submitted. Please see the
metadactyl documentation for more details.

## Updating a Tool Installation Request

Secured Endpoint: POST /admin/tool-requests/{tool-request-id}/status

Delegates to metadactyl: POST /admin/tool-requests/{tool-request-id}/status

This service is primarily a passthrough to the metadactyl endpoint using the
same path. The only difference is that this endpoint also generates a
notification for the tool request status update. Please see the metadactyl
documentation for more details.

## Listing Tool Installation Requests

Secured Endpoint: GET /tool-requests

Delegates to metadactyl: GET /tool-requests

Secured Endpoint: GET /admin/tool-requests

Delegates to metadactyl: GET /admin/tool-requests

These services are passthroughs to the metadactyl endpoints using the same path.
Please see the metadactyl documentation for more details.

## Listing Tool Installation Request Details

Secured Endpoint: GET /admin/tool-requests/{tool-request-id}

Delegates to metadactyl: GET /admin/tool-requests/{tool-request-id}

This service is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more details.

## Listing Tool Request Status Codes

Secured Endpoint: GET /tool-requests/status-codes

This service is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more details.
