# Table of Contents

* [App Metadata Information Services](#app-metadata-information-services)
    * [Getting Analyses in the JSON Format Required by the DE](#getting-analyses-in-the-json-format-required-by-the-de)
    * [Getting Analysis Details](#getting-analysis-details)
    * [Getting an App Description](#getting-an-app-description)

# App Metadata Information Services

## Getting Analyses in the JSON Format Required by the DE

*Unsecured Endpoint:* GET /get-analysis/{analysis-id}

The purpose of this endpoint is to provide a way to determine what the JSON for
an analysis will look like when it is obtained by the DE. The DE itself uses a
secured endpoint that performs the same task, but there was no reason to require
a user to be authenticated in order to obtain this information. We left this
endpoint in place despite the fact that it's not used by the DE because it's
convenient for debugging.

The response body for this service is in the following format:

```json
{
    "groups": [
        {
            "id": "property-group-id",
            "label": "property-group-label",
            "name": "property-group-name",
            "properties": [
                {
                    "description": "property-description",
                    "id": "unique-property-id",
                    "isVisible": "visibility-flag",
                    "label": "property-label",
                    "name": "property-name",
                    "type": "property-type-name",
                    "validator": {
                        "id": "validator-id",
                        "label": "validator-label",
                        "name": "validator-name",
                        "required": "required-flag",
                        "rules": [
                            {
                                "rule-type": [
                                    "rule-arg-1",
                                    "rule-arg-2",
                                    ...,
                                    "rule-arg-n"
                                ],
                            },
                            ...
                        ]
                    },
                    "value": "default-property-value"
                },
                ...
            ],
            "type": "property-group-type"
        },
        ...
    ]
    "id": "analysis-id",
    "label": "analysis-label",
    "name": "analysis-name",
    "type": "analysis-type"
}
```

Here's an example:

```
$ curl -s http://by-tor:8888/get-analysis/9BCCE2D3-8372-4BA5-A0CE-96E513B2693C | python -mjson.tool
{
    "groups": [
        {
            "id": "idPanelData1",
            "label": "Select FASTQ file",
            "name": "FASTX Trimmer - Select data:",
            "properties": [
                {
                    "description": "",
                    "id": "step_1_ta2eed78a0e924e6ba4fec03d929d905b_DE79E631-A10A-9C36-8764-506E3B2D59BD",
                    "isVisible": true,
                    "label": "Select FASTQ file:",
                    "name": "-i ",
                    "type": "FileInput",
                    "validator": {
                        "label": "",
                        "name": "",
                        "required": true
                    }
                }
            ],
            "type": "step"
        },
        ...
    ],
    "id": "9BCCE2D3-8372-4BA5-A0CE-96E513B2693C",
    "label": "FASTX Workflow",
    "name": "FASTX Workflow",
    "type": ""
}
```

*Secured Endpoint:* GET /secured/template/{analysis-id}

This service is the secured version of the `/get-analyis` endpoint. The response
body for this service is in the following format:

```json
{
    "groups": [
        {
            "id": "property-group-id",
            "label": "property-group-label",
            "name": "property-group-name",
            "properties": [
                {
                    "description": "property-description",
                    "id": "unique-property-id",
                    "isVisible": "visibility-flag",
                    "label": "property-label",
                    "name": "property-name",
                    "type": "property-type-name",
                    "validator": {
                        "id": "validator-id",
                        "label": "validator-label",
                        "name": "validator-name",
                        "required": "required-flag",
                        "rules": [
                            {
                                "rule-type": [
                                    "rule-arg-1",
                                    "rule-arg-2",
                                    ...,
                                    "rule-arg-n"
                                ],
                            },
                            ...
                        ]
                    },
                    "value": "default-property-value"
                },
                ...
            ],
            "type": "property-group-type"
        },
        ...
    ]
    "id": "analysis-id",
    "label": "analysis-label",
    "name": "analysis-name",
    "type": "analysis-type"
}
```

Here's an example:

```
curl -s "http://by-tor:8888/secured/template/9BCCE2D3-8372-4BA5-A0CE-96E513B2693C?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "groups": [
        {
            "id": "idPanelData1",
            "label": "Select FASTQ file",
            "name": "FASTX Trimmer - Select data:",
            "properties": [
                {
                    "description": "",
                    "id": "step_1_ta2eed78a0e924e6ba4fec03d929d905b_DE79E631-A10A-9C36-8764-506E3B2D59BD",
                    "isVisible": true,
                    "label": "Select FASTQ file:",
                    "name": "-i ",
                    "type": "FileInput",
                    "validator": {
                        "label": "",
                        "name": "",
                        "required": true
                    }
                }
            ],
            "type": "step"
        },
        ...
    ],
    "id": "9BCCE2D3-8372-4BA5-A0CE-96E513B2693C",
    "label": "FASTX Workflow",
    "name": "FASTX Workflow",
    "type": ""
}
```
