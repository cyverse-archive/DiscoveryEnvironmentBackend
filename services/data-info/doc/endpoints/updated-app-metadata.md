# Table of Contents

* [Updated Application Metadata Endpoints](#updated-application-metadata-endpoints)
    * [Updating or Importing a Single-Step App](#updating-or-importing-a-single-step-app)
    * [Obtaining an App Representation for Editing](#obtaining-an-app-representation-for-editing)
    * [Obtaining App Information for Job Submission](#obtaining-app-information-for-job-submission)
    * [Previewing Command Line Arguments](#previewing-command-line-arguments)

# Updated Application Metadata Endpoints

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](data-info-v-metadactyl.md) for more
information.

## Updating or Importing a Single-Step App

*Secured Endpoint:* POST /secured/update-app

*Delegates to metadactyl:* POST /secured/update-app

This endpoint is a passthrough to the metadactyl endpoint using the same
path. Please see the metadactyl documentation for more information.

## Obtaining an App Representation for Editing

*Secured Endpoint:* GET /apps/{app-id}/ui

*Delegates to metadactyl:* GET /apps/{app-id}/ui

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.

## Obtaining App Information for Job Submission

*Secured Endpoint:* GET /secured/app/{app-id}

The job submission utility in the DE uses this service to obtain a description
of the app in a format that is suitable for job submission. This JSON format is
identical to the template JSON format above, and multi-step apps are condensed
so that they appear to contain just one step.

```
$ curl -s "http://by-tor:8888/secured/app/A750DD7B-7EBC-4809-B9EC-6F717220A1D1?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "groups": [
        {
            "id": "BF640C7B-E5EA-4232-AC88-7F7D2768E3C1",
            "label": "Grp1",
            "name": "",
            "properties": [
                {
                    "arguments": [],
                    "defaultValue": null,
                    "description": "File input tool tip",
                    "id": "App Endpoint Test_2A72C63F-569A-4C6E-9572-E075F705DD3D",
                    "isVisible": true,
                    "label": "Input File",
                    "name": "-f",
                    "required": true,
                    "type": "FileInput",
                    "validators": []
                },
                {
                    "arguments": [],
                    "defaultValue": "",
                    "description": "TextBox tool tip",
                    "id": "App Endpoint Test_DA067538-535A-44FF-B927-0558DBD5E1D5",
                    "isVisible": true,
                    "label": "TextBox",
                    "name": "-b",
                    "required": false,
                    "type": "Text",
                    "validators": []
                },
                {
                    "arguments": [],
                    "defaultValue": "",
                    "description": "checkbox tool tip",
                    "id": "App Endpoint Test_01DBA927-0A02-48D2-9B85-CE77A66B2D63",
                    "isVisible": true,
                    "label": "Checkbox",
                    "name": "-c",
                    "required": false,
                    "type": "Flag",
                    "validators": []
                }
            ],
            "type": ""
        }
    ],
    "id": "A750DD7B-7EBC-4809-B9EC-6F717220A1D1",
    "label": "App Endpoint Test",
    "name": "App Endpoint Test",
    "disabled": false,
    "type": ""
}
```

## Previewing Command Line Arguments

*Unsecured Endpoint:* POST /apps/arg-preview

*Delegates to metadactyl:* POST /apps/arg-preview

This endpoint is a passthrough to the metadactyl endpoint using the same path.
Please see the metadactyl documentation for more information.
