# Table of Contents

* [Application Execution Endpoints](#application-execution-endpoints)
    * [Obtaining Property Values for a Previously Executed Job](#obtaining-property-values-for-a-previously-executed-job)
    * [Obtaining Information to Rerun a Job](#obtaining-information-to-rerun-a-job)
    * [Submitting a Job for Execution](#submitting-a-job-for-execution)
    * [Listing Jobs](#listing-jobs)
    * [Deleting Jobs](#deleting-jobs)
    * [Updating Analysis Information](#updating-analysis-information)
    * [Stopping a Running Analysis](#stopping-a-running-analysis)

# Application Execution Endpoints

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](data-info-v-metadactyl.md) for more
information.

## Obtaining Property Values for a Previously Executed Job

Secured Endpoint: GET /secured/get-property-values/{job-id}

This service is used to obtain the property values that were passed to a previously submitted job.
For Agave jobs, the information in the response body is obtained from various Agave endpoints.
For DE jobs, the information in the response body is obtained from DE app info
and a potential combination of Agave app info (in the case of pipelines including Agave apps).

The response body is in the following format:

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
$ curl -s http://by-tor:8888/secured/get-property-values/6DA82049-93A7-483B-8E21-36D84AFCC29F?proxyToken=$(cas-ticket) | python -mjson.tool{
    "analysis_id": "t5b1ca8927563499ba919675ae434fddf",
    "parameters": [
        {
            "data_format": "Unspecified",
            "full_param_id": "Detect text file types_91110D85-C0BC-4AFB-A1C6-0C90B0A4BA92",
            "info_type": "File",
            "is_default_value": false,
            "is_visible": true,
            "param_id": "99229E18-4C91-4512-B459-35CD316ACC25",
            "param_name": "Files to examine",
            "param_type": "Input",
            "param_value": {
                "value": "/iplant/home/dennis/50K_final_newick.tre"
            }
        },
        {
            "data_format": "Unspecified",
            "full_param_id": "Detect text file types_91110D85-C0BC-4AFB-A1C6-0C90B0A4BA92",
            "info_type": "File",
            "is_default_value": false,
            "is_visible": true,
            "param_id": "114154A7-F3C6-44CD-B443-594582761792",
            "param_name": "Files to examine",
            "param_type": "Input",
            "param_value": {
                "value": "/iplant/home/dennis/accepted_hits_10k.sam"
            }
        },
        {
            "data_format": "",
            "full_param_id": "Detect text file types_519583DD-1850-4432-AC6B-85E171654A3D",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "519583DD-1850-4432-AC6B-85E171654A3D",
            "param_name": "Sample size",
            "param_type": "Integer",
            "param_value": {
                "value": "1000"
            }
        },
        {
            "data_format": "Unspecified",
            "full_param_id": "Detect text file types_085FB3DD-30E6-4187-8033-ECA57CF72EF0",
            "info_type": "PlainText",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "085FB3DD-30E6-4187-8033-ECA57CF72EF0",
            "param_name": "Output file name",
            "param_type": "Output",
            "param_value": {
                "value": "types.txt"
            }
        }
    ],
    "success": true
}
```

## Obtaining Information to Rerun a Job

*Secured Endpoint:* GET /secured/app-rerun-info/{job-id}

It's occasionally nice to be able to rerun a job that was prevously executed,
possibly with some tweaked values. The UI uses this service to obtain analysis
information in the same format as the `/app/{analysis-id}` service with the
property values from a specific job plugged in.

For Agave jobs, the information in the response body is obtained from various Agave endpoints.
For DE jobs, the information in the response body is obtained from DE app info
and a potential combination of Agave app info (in the case of pipelines including Agave apps).

Here's an example:

```
$ curl -s http://by-tor:8888/app-rerun-info/D3AE0C5C-CC74-4A98-8D26-224D6366F9D6?proxyTicket=$(cas-ticket) | python -mjson.tool
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

Secured Endpoint: PUT /secured/workspaces/{workspace-id}/newexperiment

Delegates to metadactyl: PUT /secured/workspaces/{workspace-id}/newexperiment
Or submits a job to Foundation API.

This endpoint is a passthrough to the metadactyl endpoint using the same
path, or submits a job to Foundation API.

The response body for this service is in the following format:

```json
{
    "success": true,
    "id": "job-id",
    "name": "Job Name",
    "status": "Submitted",
    "start-date": start-date-as-milliseconds-since-epoch
}
```

Please see the
[metadactyl documentation](http://github.com/iPlantCollaborativeOpenSource/metadactyl-clj/blob/master/doc/endpoints/app-execution.md#submitting-a-job-for-execution)
for more information, including the request format.

## Listing Jobs

*Secured Endpoint:* GET /secured/workspaces/{workspace-id}/executions/list

Information about the status of jobs that have previously been submitted for
execution can be obtained using this service. The DE uses this service to
populate the _Analyses_ window. The response body for this service is in the
following format:

```json
{
    "analyses": [
        {
            "analysis_details": "analysis-description",
            "analysis_id": "analysis-id",
            "analysis_name": "analysis-name",
            "app_disabled": false,
            "description": "job-description",
            "enddate": "end-date-as-milliseconds-since-epoch",
            "id": "job-id",
            "name": "job-name",
            "resultfolderid": "path-to-result-folder",
            "startdate": "start-date-as-milliseconds-since-epoch",
            "status": "job-status-code",
            "wiki_url": "analysis-documentation-link"
        },
        ...
    ],
    "success": true,
    "timestamp": "timestamp",
    "total": "total"
}
```

With no query string parameters aside from `user` and `email`, this service
returns information about all jobs ever run by the user that haven't been marked
as deleted in descending order by start time (that is, the `startdate` field in
the result). Several query-string parameters are available to alter the way this
service behaves:

| Name | Description | Default |
| ---- | ----------- | ------- |
| limit | The maximum number of results to return. If this value is zero or negative then all results will be returned. | 0 |
| offset | The index of the first result to return. | 0 |
| sort-field | The name of the field that results are sorted by. Valid values for this parameter are `name`, `analysis_name`, `startdate`, `enddate`, and `status`. | startdate |
| sort-order | `asc` or `ASC` for ascending and `desc` or `DESC` for descending. | desc |
| filter | Allows results to be filtered based on the value of some result field.  The format of this parameter is `[{"field":"some_field", "value":"search-term"}, ...]`, where `field` is the name of the field on which the filter is based and `value` is the search value. If `field` is `name` or `analysis_name`, then `value` can be contained anywhere, case-insensitive, in the corresponding field. For example, to obtain the list of all jobs that were executed using an application with `CACE` anywhere in its name, the parameter value can be `[{"field":"analysis_name","value":"cace"}]`. To find a job with a specific `id`, the parameter value can be `[{"field":"id","value":"C09F5907-B2A2-4429-A11E-5B96F421C3C1"}]`. Additional filters may be provided in the query array, and any analysis that matches any filter will be returned. | No filtering |

Note that the JSON value used by the filter parameter can potentially contain
characters that must be URL encoded. For example, the URL encoded version of
`[{"field":"analysis_name","value":"cace"}]` would be:

```
%5B%7B%22field%22%3A%22analysis_name%22%2C%22value%22%3A%22cace%22%7D%5D
```

Of course, this is a pain to type in, for example, a `curl` command. If you're
calling the service using curl then a bash function that encodes strings for you
will be very helpful. If you have a recent version of Python installed then this
function will work:

```bash
function urlencode {
    python -c "import urllib;import sys;print urllib.quote_plus(sys.argv[1])" "$1"
}
```

With this function defined, a `curl` command to call this service with a filter
can be simplified to something like this:

```
curl -s "http://by-tor:8888/secured/workspaces/4/executions/list?proxyToken=$(cas-ticket)&filter=$(urlencode '[{"field":"analysis_name","value":"cace"}]')" | python -mjson.tool
```

Here's an example using no parameters:

```
$ curl -s "http://by-tor:8888/secured/workspaces/4/executions/list?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "analyses": [
        {
            "analysis_details": "Count words in a file",
            "analysis_id": "wc-1.00u1",
            "analysis_name": "Word Count",
            "app-disabled": false,
            "description": "",
            "enddate": "1382979411000",
            "id": "31499",
            "name": "wc_10280954",
            "resultfolderid": "/iplant/home/snow-dog/analyses/wc_10280954",
            "startdate": "1382979299935",
            "status": "Completed",
            "wiki_url": ""
        },
        ...
    ],
    "success": true,
    "timestamp": "1383000130668",
    "total": 23
}
```

Here's an example of a search with a limit of one result:

```
$ curl -s "http://by-tor:8888/secured/workspaces/4/executions/list?proxyToken=$(cas-ticket)&limit=1" | python -mjson.tool
{
    "analyses": [
        {
            "analysis_details": "Count words in a file",
            "analysis_id": "wc-1.00u1",
            "analysis_name": "Word Count",
            "app-disabled": false,
            "description": "",
            "enddate": "1382979411000",
            "id": "31499",
            "name": "wc_10280954",
            "resultfolderid": "/iplant/home/snow-dog/analyses/wc_10280954",
            "startdate": "1382979299935",
            "status": "Completed",
            "wiki_url": ""
        }
    ],
    "success": true,
    "timestamp": "1383000130668",
    "total": 23
}
```

## Deleting Jobs

*Secured Endpoint:* PUT /secured/workspaces/{workspace-id}/executions/delete

After a job has completed, a user may not want to view the job status
information in the _Analyses_ window any longer. This service provides a way to
mark job status information as deleted so that it no longer shows up. The
request body for this service is in the following format:

```json
{
    "executions": [
        "job-id-1",
        "job-id-2",
        ...,
        "job-id-n"
    ]
}
```

The response body for this endpoint contains only a status flag if the service
succeeds.

It should be noted that this service does not fail if any of the job identifiers
refers to a non-existent or deleted job. If the identifier refers to a deleted
job then the update is essentially a no-op. If a job with the identifier can't
be found then a warning message is logged in metadactyl-clj's log file, but the
service does not indicate that a failure has occurred.

Here's an example:

```
$ curl -X PUT -sd '
{
    "executions": [
        "84DFCC0E-03B9-4DF4-8484-55BFBD6FE841",
        "FOO"
    ]
}
' "http://by-tor:8888/secured/workspaces/4/executions/delete?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true
}
```

## Updating Analysis Information

*Secured Endpoint:* PATCH /secured/analysis/{analysis-id}

This endpoint allows an analysis name or description to be updated. The request
body is in the following format:

```json
{
    "name": "new analysis name",
    "description": "new analysis description",
}
```
Neither field is required; if both fields are omitted then this service is a
no-op; no error will be thrown. If the update is successful, the job listing
will be included in the response body. Here's an example:

```
$ curl -sX PATCH "http://by-tor:8888/secured/analysis/2725F72B-2EC9-4FB8-BF72-05136B5D71F4?proxyToken=$(cas-ticket)" -d '
{
    "description": "One word! Two words! Three! Three words! Ah, ah, ah!",
    "name": "obsessive_word_count"
}
' | python -mjson.tool
{
    "app_name": "Word Count",
    "deleted": false,
    "end_date": "2014-05-10T04:03:32Z",
    "external_id": "2725F72B-2EC9-4FB8-BF72-05136B5D71F4",
    "id": "9d63e97f-6bb4-4237-aa01-59238e1a4d89",
    "job_description": "One word! Two words! Three! Three words! Ah, ah, ah!",
    "job_name": "obsessive_word_count",
    "job_type_id": 1,
    "start_date": "2014-05-10T04:02:58Z",
    "status": "Completed",
    "success": true,
    "user_id": 2
}
```

## Stopping a Running Analysis

Secured Endpoint: DELETE /secured/stop-analysis/{job-id}

This service stops a running analysis or pipeline. The `job-id` component of
the URL is the primary key from the `jobs` table in the DE database, which is
also the job identifier included in the analysis listing. An error will occur if
the job is not found, the currently authenticated user is not the one who
submitted the job or if the job is already in one of the completed statuses
(`Completed`, `Failed` or `Canceled`).

Assuming the job is found and is not in one of the completed statuses, the
service sets the job status to `Canceled` then searches for the first incomplete
step associated with the job. If no incomplete step is found then the service
calls either the JEX or Agave to stop the currently running step. Note that
failure to cancel a job step does not cause the service to return an
error. Finally, the status of the job step is set to `Canceled` in the
database.

One of the edge cases to consider here is a potential race condition between
this service and the completion of the currently running job step. The risk of a
race condition is minimized here by changing the job status to `Canceled` before
attempting to cancel the most recently submitted job step. This allows the job
status update service to detect when a job has been marked as canceled and abort
before submitting the next step in the pipeline.

If an error occurs then the response body for this service is in the standard
error format. Otherwise, the response body contains a success indicator along
with the job identifier. Here's an example of a successful service call:

```
$ curl -X DELETE -s "http://by-tor:8888/secured/stop-analysis/21dada70-acc3-4967-9c3f-73133e56724a?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "id": "21dada70-acc3-4967-9c3f-73133e56724a",
    "success": true
}
```

Here's an example of an unsuccessful service call:

```
$ curl -X DELETE -s "http://by-tor:8888/secured/stop-analysis/21dada70-acc3-4967-9c3f-73133e56724a?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "error_code": "ERR_BAD_REQUEST",
    "reason": "job, 21dada70-acc3-4967-9c3f-73133e56724a, is already completed or canceled",
    "success": false
}
```
