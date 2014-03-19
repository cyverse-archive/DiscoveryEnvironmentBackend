# Table of Contents

* [App Execution Endpoints](#app-execution-endpoints)
    * [Obtaining Property Values for a Previously Executed Job](#obtaining-property-values-for-a-previously-executed-job)
    * [Obtaining Information to Rerun a Job](#obtaining-information-to-rerun-a-job)
    * [Submitting a Job for Execution](#submitting-a-job-for-execution)

# App Execution Endpoints

## Obtaining Property Values for a Previously Executed Job

*Unsecured Endpoint:* GET /get-property-values/{job-id}

This service obtains the property values that were passed to a job that has
already been executed so that the user can see which values were passed to the
job. The response body is in the following format:

```json
{
    "analysis_id": "analysis-id",
    "parameters": [
        {
            "full_param_id": "fully-qualified-parameter-id",
            "param_id": "parameter-id",
            "param_name": "parameter-name",
            "param_value": {
                "value": "parameter-value"
            },
            "param_type": "parameter-type",
            "info_type": "info-type-name",
            "data_format": "data-format-name",
            "is_default_value": "default-value-flag",
            "is_visible": "visibility-flag"
        },
        ...
    ]
}
```

Note that the information type and data format only apply to input files. For
other types of parameters, these fields will be blank. The `is_default_value`
flag indicates whether or not the default value was used in the job submission.
The value of this flag is determined by comparing the actual property value
listed in the job submission to the default property value in the application
definition. If the default value in the application definition is not blank and
the actual value equals the default value then this flag will be set to `true`.
Otherwise, this flag will be set to `false`. The `is_visible` flag indicates
whether or not the property is visible in the user interface for the
application. This value is copied directly from the application definition.

Here's an example:

```
$ curl -s http://by-tor:8888/get-property-values/jebf8120d-0ccb-45d1-bae6-849620f31553 | python -mjson.tool
{
    "analysis_id": "t55e2377c60724ecbbcfa1a39c9ef1eec",
    "parameters": [
        {
            "data_format": "Unspecified",
            "info_type": "File",
            "is_default_value": false,
            "is_visible": true,
            "full_param_id": "step1_38950035-8F31-0A27-1BE1-8E55F5C30B54",
            "param_id": "38950035-8F31-0A27-1BE1-8E55F5C30B54",
            "param_name": "Select an SRA or SRAlite file:",
            "param_type": "Input",
            "param_value": {
                "value": ""/iplant/home/nobody/SRR001355.lite.sra"
            }
        },
        {
            "data_format": "",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "full_param_id": "step1_B962E548-4023-E40C-48E5-6484AF55E5DD",
            "param_id": "B962E548-4023-E40C-48E5-6484AF55E5DD",
            "param_name": "Optional accession override",
            "param_type": "Text",
            "param_value": {
                "value": ""
            }
        },
        {
            "data_format": "",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "full_param_id": "step1_DCFC3CD9-FB31-E0F8-C4CB-78F66FF368D2",
            "param_id": "DCFC3CD9-FB31-E0F8-C4CB-78F66FF368D2",
            "param_name": "File contains paired-end data",
            "param_type": "Flag",
            "param_value": {
                "value": "true"
            }
        },
        {
            "data_format": "",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "full_param_id": "step1_0E21A202-EC8A-7BFD-913B-FA73FE86F58E",
            "param_id": "0E21A202-EC8A-7BFD-913B-FA73FE86F58E",
            "param_name": "Offset to use for quality scale conversion",
            "param_type": "Number",
            "param_value": {
                "value": "33"
            }
        },
        {
            "data_format": "",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "full_param_id": "step1_F9AD602D-38E3-8C90-9DD7-E1BB4971CD70",
            "param_id": "F9AD602D-38E3-8C90-9DD7-E1BB4971CD70",
            "param_name": "Emit only FASTA records without quality scores",
            "param_type": "Flag",
            "param_value": {
                "value": "false"
            }
        },
        {
            "data_format": "",
            "info_type": "",
            "is_default_value": true,
            "is_visible": false,
            "full_param_id": "step1_6BAD8D7F-3EE2-A52A-93D1-1329D1565E4F",
            "param_id": "6BAD8D7F-3EE2-A52A-93D1-1329D1565E4F",
            "param_name": "Verbose",
            "param_type": "Flag",
            "param_value": {
                "value": "true"
            }
        }
    ]
}
```

## Obtaining Information to Rerun a Job

*Unsecured Endpoint:* GET /app-rerun-info/{job-id}

It's occasionally nice to be able to rerun a job that was prevously executed,
possibly with some tweaked values. The UI uses this service to obtain analysis
information in the same format as the `/get-app/{analysis-id}` service with the
property values from a specific job plugged in. Here's an example:

```
$ curl -s http://by-tor:8888/app-rerun-info/D3AE0C5C-CC74-4A98-8D26-224D6366F9D6 | python -mjson.tool
{
    "disabled": false,
    "groups": [
        {
            "id": "63D82E2F-30AC-426E-87BB-724FB148A1B0",
            "label": "Input",
            "name": "",
            "properties": [
                {
                    "arguments": [],
                    "defaultValue": {
                        "value": [
                            "/iplant/home/nobody/clojure-keybindings.el"
                        ]
                    },
                    "description": "Select the files to concatenate.",
                    "id": "Jaguarundi_3FC12E6D-63C1-49DF-8E8F-60819BCB69E3",
                    "isVisible": true,
                    "label": "Files",
                    "name": "",
                    "required": true,
                    "type": "MultiFileSelector",
                    "validators": []
                }
            ],
            "type": ""
        },
        {
            "id": "9FBCACA8-1070-4F50-9E59-E4B86AA46549",
            "label": "Options",
            "name": "",
            "properties": [
                {
                    "arguments": [
                        {
                            "display": "No special formatting.",
                            "name": " ",
                            "value": ""
                        },
                        {
                            "display": "Number output lines.",
                            "name": "-n",
                            "value": ""
                        },
                        {
                            "display": "Number non-blank output lines.",
                            "name": "-b",
                            "value": ""
                        }
                    ],
                    "defaultValue": {
                        "display": "No special formatting.",
                        "name": " ",
                        "value": ""
                    },
                    "description": "Select the display mode for the output.",
                    "id": "Jaguarundi_6F32FBAD-4836-4C56-BF37-3A2D0547450D",
                    "isVisible": true,
                    "label": "Line Numbering",
                    "name": "",
                    "required": true,
                    "type": "Selection",
                    "validators": []
                },
                {
                    "arguments": [
                        {
                            "display": "Do not display.",
                            "name": " ",
                            "value": ""
                        },
                        {
                            "display": "Display non-whitespace characters.",
                            "name": "-v",
                            "value": ""
                        },
                        {
                            "display": "Display non-whitespace and tab characters.",
                            "name": "-t",
                            "value": ""
                        },
                        {
                            "display": "Display non-whitespace characters and end-of-line markers.",
                            "name": "-e",
                            "value": ""
                        }
                    ],
                    "defaultValue": {
                        "display": "Do not display.",
                        "name": " ",
                        "value": ""
                    },
                    "description": "Options for displaying non-printable characters.",
                    "id": "Jaguarundi_C1393000-E8E3-4FA3-9A4B-6CF858057FA1",
                    "isVisible": true,
                    "label": "Non-Printing Characters",
                    "name": "",
                    "required": false,
                    "type": "Selection",
                    "validators": []
                }
            ],
            "type": ""
        }
    ],
    "id": "A00D750F-D8B3-4169-B976-FAAA161CB3E3",
    "label": "Jaguarundi",
    "name": "Jaguarundi",
    "success": true,
    "type": ""
}
```

## Submitting a Job for Execution

*Secured Endpoint:* PUT /secured/workspaces/{workspace-id}/newexperiment

The DE uses this service to submit jobs for execution on behalf of the user. The
request body is in the following format:

```json
{
    "config": {
        property-id-1: "property-value-1",
        property-id-2: "property-value-2",
        ...,
        property-id-n: "property-value-n"
    },
    "analysis_id": "analysis-id",
    "name": "job-name",
    "type": "job-type",
    "debug": "debug-flag",
    "workspace_id": "workspace-id",
    "notify": "email-notifications-enabled-flag",
    "output_dir": "output-directory-path",
    "create_output_subdir": "auto-create-subdir-flag",
    "description": "job-description"
}
```

The property identifiers deserve some special mention here because they're not
obtained directly from the database. If you examine the output from the
`/get-analysis/{analysis-id}` endpoint or the `/template/{analysis-id}` endpoint
then these property identifiers are the ones that show up in the service output.
If you're looking in the database (or in the output from the
`/export-workflow/{analysis-id}` endpoint) then you can obtain the property ID
used in this service by combining the step name, a literal underscore and the
actual property identifier.

This service produces a response body consisting of a single JSON object
containing the job status information. Here's an example:

```
$ curl -XPUT -sd '
{
 "config":   {
   "step_1_LastLines": "1",
   "step_1_6FF31B1C-3DAB-499C-8521-69227C52CE10": "/iplant/home/snow-dog/data_files/aquilegia-tree.txt"
 },
 "analysis_id": "aa54b4fd9b56545db978fff4398c5ce81",
 "name": "a1",
 "type": "Text Manipulation",
 "debug": false,
 "workspace_id": "4",
 "notify": true,
 "output_dir": "/iplant/home/snow-dog/sharewith",
 "create_output_subdir": true,
 "description": ""
}
' http://by-tor:8888/secured/workspaces/4/newexperiment?user=snow-dog | python -mjson.tool
{
   "analysis_details": "Extracts a specified number of lines from the beginning of file",
   "analysis_id": "aa54b4fd9b56545db978fff4398c5ce81",
   "analysis_name": "Extract First Lines From a File",
   "description": "",
   "enddate": "0",
   "id": "jf7600670-ed13-46cd-8810-dfddb075d819",
   "name": "a1",
   "resultfolderid": "/iplant/home/snow-dog/sharewith/a1-2012-10-10-18-44-47.548",
   "startdate": "1349919887549",
   "status": "Submitted",
   "success": true,
   "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Extract%20First%20Lines%20From%20a%20File"
}
```
