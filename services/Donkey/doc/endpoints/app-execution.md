# Table of Contents

* [Application Execution Endpoints](#application-execution-endpoints)
    * [Obtaining Property Values for a Previously Executed Job](#obtaining-property-values-for-a-previously-executed-job)
    * [Obtaining Information to Rerun a Job](#obtaining-information-to-rerun-a-job)
    * [Submitting a Job for Execution](#submitting-a-job-for-execution)
    * [Listing Jobs](#listing-jobs)
    * [Deleting a Job](#deleting-a-job)
    * [Deleting Multiple Jobs](#deleting-multiple-jobs)
    * [Updating Analysis Information](#updating-analysis-information)
    * [Stopping a Running Analysis](#stopping-a-running-analysis)

# Application Execution Endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Obtaining Parameter Values for a Previously Executed Job

Secured Endpoint: GET /analyses/{analysis-id}/parameters

This service is used to obtain the parameter values that were passed to a previously submitted job.
For Agave jobs, the information in the response body is obtained from various Agave endpoints.  For
DE jobs, the information in the response body is obtained from DE app info and a potential
combination of Agave app info (in the case of pipelines including Agave apps).

The response body is in the following format:

```json
{
    "app_id": "app-id",
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

Note that the information type and data format only apply to input files. For other types of
parameters, these fields will be blank. The `is_default_value` flag indicates whether or not the
default value was used in the job submission.  The value of this flag is determined by comparing the
actual property value listed in the job submission to the default property value in the application
definition. If the default value in the application definition is not blank and the actual value
equals the default value then this flag will be set to `true`.  Otherwise, this flag will be set to
`false`. The `is_visible` flag indicates whether or not the property is visible in the user
interface for the application. This value is copied directly from the application definition.

Here's an example:

```
$ curl -s "http://by-tor:8888/analyses/5b46cfc7-c363-4aaa-9260-b3b6bfe7177b/parameters?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "app_id": "1effb48e-fca8-43ef-92d1-99ed636b2d13",
    "parameters": [
        {
            "data_format": "Unspecified",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_3231fbf0-7eb5-4f87-85cd-6dfa78fa7f6d",
            "info_type": "File",
            "is_default_value": false,
            "is_visible": true,
            "param_id": "3231fbf0-7eb5-4f87-85cd-6dfa78fa7f6d",
            "param_name": "Select data file",
            "param_type": "FileInput",
            "param_value": {
                "value": "/iplant/home/snow-dog/foo.csv"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_a642534f-dee2-4e36-9fa4-fe058827f89f",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "a642534f-dee2-4e36-9fa4-fe058827f89f",
            "param_name": "Enter column number for X",
            "param_type": "Integer",
            "param_value": {
                "value": "1"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_3e31f9a8-47f9-43f7-949d-86da57fe9c05",
            "info_type": "",
            "is_default_value": true,
            "is_visible": false,
            "param_id": "3e31f9a8-47f9-43f7-949d-86da57fe9c05",
            "param_name": "Select the estimated MAXIMUM memory needed",
            "param_type": "Text",
            "param_value": {
                "value": "2"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_ccf14c7d-58b5-4b88-af5a-98d73e7b71ac",
            "info_type": "",
            "is_default_value": false,
            "is_visible": true,
            "param_id": "ccf14c7d-58b5-4b88-af5a-98d73e7b71ac",
            "param_name": "Select plot type",
            "param_type": "TextSelection",
            "param_value": {
                "value": {
                    "display": "Point",
                    "id": "ef8218f4-549b-11e4-86a1-72d2050a2b40",
                    "isDefault": true,
                    "name": "--type=",
                    "value": "0"
                }
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_dc69d3a8-0d6b-43f4-add8-223373b98daf",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "dc69d3a8-0d6b-43f4-add8-223373b98daf",
            "param_name": "Enter column number for Y",
            "param_type": "Integer",
            "param_value": {
                "value": "2"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_c94b650a-e1d4-4165-b19c-f2eec849a6ad",
            "info_type": "",
            "is_default_value": true,
            "is_visible": false,
            "param_id": "c94b650a-e1d4-4165-b19c-f2eec849a6ad",
            "param_name": "Select the estimated MAXIMUM run time",
            "param_type": "Text",
            "param_value": {
                "value": "1:00:00"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_c6b7803a-6fd8-4a28-bf6d-e62662e39cee",
            "info_type": "",
            "is_default_value": true,
            "is_visible": false,
            "param_id": "c6b7803a-6fd8-4a28-bf6d-e62662e39cee",
            "param_name": "appid",
            "param_type": "Text",
            "param_value": {
                "value": "xyplot-0.0.0u1"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_3ca2a187-d3b9-47cb-a7b6-8536548f73d8",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "3ca2a187-d3b9-47cb-a7b6-8536548f73d8",
            "param_name": "Enter X label",
            "param_type": "Text",
            "param_value": {
                "value": "X"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_0433c769-f3d6-48fc-b9d2-44b2a0239588",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "0433c769-f3d6-48fc-b9d2-44b2a0239588",
            "param_name": "Enter output filename",
            "param_type": "Text",
            "param_value": {
                "value": "out.png"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_b3d30303-30d9-4bb5-afdd-fb12c1ff2272",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "b3d30303-30d9-4bb5-afdd-fb12c1ff2272",
            "param_name": "Enter Y label",
            "param_type": "Text",
            "param_value": {
                "value": "Y"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_9a3057a7-9be8-4b11-a9ef-258ec7bb4cf2",
            "info_type": "",
            "is_default_value": true,
            "is_visible": true,
            "param_id": "9a3057a7-9be8-4b11-a9ef-258ec7bb4cf2",
            "param_name": "Enter title of the plot",
            "param_type": "Text",
            "param_value": {
                "value": "2D Plot"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_cae821b0-004f-4aa2-a2b8-38f374bd180c",
            "info_type": "",
            "is_default_value": true,
            "is_visible": false,
            "param_id": "cae821b0-004f-4aa2-a2b8-38f374bd180c",
            "param_name": "Number of processors",
            "param_type": "Text",
            "param_value": {
                "value": "16"
            }
        },
        {
            "data_format": "",
            "full_param_id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_ab11ef6e-7d0c-4a22-9418-a911bc87283d",
            "info_type": "",
            "is_default_value": false,
            "is_visible": true,
            "param_id": "ab11ef6e-7d0c-4a22-9418-a911bc87283d",
            "param_name": "Select transformation method on Y",
            "param_type": "TextSelection",
            "param_value": {
                "value": {
                    "display": "No transform",
                    "id": "ef6a19a2-549b-11e4-862e-72d2050a2b40",
                    "name": "--logt=",
                    "value": "0"
                }
            }
        }
    ]
}
```

## Obtaining Information to Rerun a Job

*Secured Endpoint:* GET /analyses/{analysis-id}/relaunch-info

It's occasionally nice to be able to rerun a job that was prevously executed, possibly with some
tweaked values. The UI uses this service to obtain analysis information in the same format as the
`/apps/{app-id}` service with the property values from a specific job plugged in.

For Agave jobs, the information in the response body is obtained from various Agave endpoints.  For
DE jobs, the information in the response body is obtained from DE app info and a potential
combination of Agave app info (in the case of pipelines including Agave apps).

Here's an example:

```
$ curl -s "http://by-tor:8888/analyses/5b46cfc7-c363-4aaa-9260-b3b6bfe7177b/relaunch-info?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "categories": [],
    "description": "Generate XY plot from column-wise data file",
    "disabled": false,
    "groups": [
        {
            "id": "6a757557-5171-4d8b-978c-784121d44eda",
            "label": "Input",
            "name": "",
            "parameters": [
                {
                    "arguments": [],
                    "defaultValue": "/iplant/home/snow-dog/foo.csv",
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_3231fbf0-7eb5-4f87-85cd-6dfa78fa7f6d",
                    "isVisible": true,
                    "label": "Select data file",
                    "name": "--i=",
                    "type": "FileInput",
                    "validators": [],
                    "value": "/iplant/home/snow-dog/foo.csv"
                }
            ],
            "step_number": 0
        },
        {
            "id": "dd05d6a3-c9de-41fd-b06f-6c7cc742933b",
            "label": "Options",
            "name": "",
            "parameters": [
                {
                    "arguments": [],
                    "defaultValue": 1,
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_a642534f-dee2-4e36-9fa4-fe058827f89f",
                    "isVisible": true,
                    "label": "Enter column number for X",
                    "name": "--xcol=",
                    "type": "Integer",
                    "validators": [],
                    "value": 1
                },
                {
                    "arguments": [],
                    "defaultValue": 2,
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_dc69d3a8-0d6b-43f4-add8-223373b98daf",
                    "isVisible": true,
                    "label": "Enter column number for Y",
                    "name": "--ycol=",
                    "type": "Integer",
                    "validators": [],
                    "value": 2
                },
                {
                    "arguments": [],
                    "defaultValue": "X",
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_3ca2a187-d3b9-47cb-a7b6-8536548f73d8",
                    "isVisible": true,
                    "label": "Enter X label",
                    "name": "--xlabel=",
                    "type": "Text",
                    "validators": [],
                    "value": "X"
                },
                {
                    "arguments": [],
                    "defaultValue": "Y",
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_b3d30303-30d9-4bb5-afdd-fb12c1ff2272",
                    "isVisible": true,
                    "label": "Enter Y label",
                    "name": "--ylabel=",
                    "type": "Text",
                    "validators": [],
                    "value": "Y"
                },
                {
                    "arguments": [],
                    "defaultValue": "2D Plot",
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_9a3057a7-9be8-4b11-a9ef-258ec7bb4cf2",
                    "isVisible": true,
                    "label": "Enter title of the plot",
                    "name": "--title=",
                    "type": "Text",
                    "validators": [],
                    "value": "2D Plot"
                },
                {
                    "arguments": [
                        {
                            "display": "No transform",
                            "id": "ef6a19a2-549b-11e4-862e-72d2050a2b40",
                            "isDefault": false,
                            "name": "--logt=",
                            "value": "0"
                        },
                        {
                            "display": "- log10",
                            "id": "ef78c56a-549b-11e4-8673-72d2050a2b40",
                            "isDefault": true,
                            "name": "--logt=",
                            "value": "1"
                        }
                    ],
                    "defaultValue": {
                        "display": "No transform",
                        "id": "ef6a19a2-549b-11e4-862e-72d2050a2b40",
                        "name": "--logt=",
                        "value": "0"
                    },
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_ab11ef6e-7d0c-4a22-9418-a911bc87283d",
                    "isVisible": true,
                    "label": "Select transformation method on Y",
                    "name": "",
                    "type": "TextSelection",
                    "validators": [],
                    "value": {
                        "display": "No transform",
                        "id": "ef6a19a2-549b-11e4-862e-72d2050a2b40",
                        "name": "--logt=",
                        "value": "0"
                    }
                },
                {
                    "arguments": [
                        {
                            "display": "Line",
                            "id": "ef78f454-549b-11e4-8674-72d2050a2b40",
                            "isDefault": false,
                            "name": "--type=",
                            "value": "1"
                        },
                        {
                            "display": "Point",
                            "id": "ef8218f4-549b-11e4-86a1-72d2050a2b40",
                            "isDefault": true,
                            "name": "--type=",
                            "value": "0"
                        }
                    ],
                    "defaultValue": {
                        "display": "Point",
                        "id": "ef8218f4-549b-11e4-86a1-72d2050a2b40",
                        "isDefault": true,
                        "name": "--type=",
                        "value": "0"
                    },
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_ccf14c7d-58b5-4b88-af5a-98d73e7b71ac",
                    "isVisible": true,
                    "label": "Select plot type",
                    "name": "",
                    "type": "TextSelection",
                    "validators": [],
                    "value": {
                        "display": "Point",
                        "id": "ef8218f4-549b-11e4-86a1-72d2050a2b40",
                        "isDefault": true,
                        "name": "--type=",
                        "value": "0"
                    }
                }
            ],
            "step_number": 0
        },
        {
            "id": "7cb06d5d-ca07-4537-837c-e54883b1680f",
            "label": "Output",
            "name": "",
            "parameters": [
                {
                    "arguments": [],
                    "defaultValue": "out.png",
                    "description": "",
                    "id": "5e9cdf1e-5495-11e4-8981-72d2050a2b40_0433c769-f3d6-48fc-b9d2-44b2a0239588",
                    "isVisible": true,
                    "label": "Enter output filename",
                    "name": "--export=",
                    "type": "Text",
                    "validators": [],
                    "value": "out.png"
                }
            ],
            "step_number": 0
        }
    ],
    "id": "1effb48e-fca8-43ef-92d1-99ed636b2d13",
    "label": "XYPlot 0.0.1",
    "name": "XYPlot 0.0.1"
}
```

## Submitting a Job for Execution

Secured Endpoint: POST /analyses

For DE jobs, this endpoint forwards the request to metadactyl. For Agave jobs,
this endpoint forwards a request to Agave. For jobs containing both DE and Agave
job steps, this endpoint splits the job into multiple components and coordinates
job submissions to both systems.

The response body for this service is in the following format:

```json
{
    "id": "job-id",
    "name": "Job Name",
    "status": "Submitted",
    "start-date": start-date-as-milliseconds-since-epoch
}
```

Please see the metadactyl documentation for information about the request body
format.

## Listing Jobs

*Secured Endpoint:* GET /analyses

Information about the status of jobs that have previously been submitted for
execution can be obtained using this service. The DE uses this service to
populate the _Analyses_ window. The response body for this service is in the
following format:

```json
{
    "analyses": [
        {
            "app_description": "analysis-description",
            "app_id": "analysis-id",
            "app_name": "analysis-name",
            "app_disabled": false,
            "description": "job-description",
            "enddate": "end-date-as-milliseconds-since-epoch",
            "id": "job-id",
            "name": "job-name",
            "resultfolderid": "path-to-result-folder",
            "startdate": "start-date-as-milliseconds-since-epoch",
            "status": "job-status-code",
            "username": "fully-qualified-username",
            "wiki_url": "analysis-documentation-link",
            "batch": "batch-flag",
            "parent_id": "parent-identifier"
        },
        ...
    ],
    "timestamp": "timestamp",
    "total": "total"
}
```

With no query string parameters aside from the authentication token, this service returns
information about all jobs ever run by the user that haven't been marked as deleted in descending
order by start time (that is, the `startdate` field in the result). Several query-string parameters
are available to alter the way this service behaves:

| Name | Description | Default |
| ---- | ----------- | ------- |
| limit | The maximum number of results to return. If this value is zero or negative then all results will be returned. | 0 |
| offset | The index of the first result to return. | 0 |
| sort-field | The name of the field that results are sorted by. Valid values for this parameter are `name`, `app_name`, `startdate`, `enddate`, and `status`. | startdate |
| sort-order | `asc` or `ASC` for ascending and `desc` or `DESC` for descending. | desc |
| filter | Allows results to be filtered based on the value of some result field.  The format of this parameter is `[{"field":"some_field", "value":"search-term"}, ...]`, where `field` is the name of the field on which the filter is based and `value` is the search value. If `field` is `name` or `app_name`, then `value` can be contained anywhere, case-insensitive, in the corresponding field. For example, to obtain the list of all jobs that were executed using an application with `CACE` anywhere in its name, the parameter value can be `[{"field":"app_name","value":"cace"}]`. To find a job with a specific `id`, the parameter value can be `[{"field":"id","value":"C09F5907-B2A2-4429-A11E-5B96F421C3C1"}]`. To find jobs associated with a specific `parent_id`, the parameter value can be `[{"field":"parent_id","value":"b4c2f624-7cbd-496e-adad-5be8d0d3b941"}]`. It's also possible to search for jobs without a parent using this parameter value: `[{"field":"parent_id","value":null}]`. |

Note that the JSON value used by the filter parameter can potentially contain characters that must
be URL encoded. For example, the URL encoded version of `[{"field":"app_name","value":"cace"}]`
would be:

```
%5B%7B%22field%22%3A%22app_name%22%2C%22value%22%3A%22cace%22%7D%5D
```

Of course, this is a pain to type in, for example, a `curl` command. If you're calling the service
using curl then a bash function that encodes strings for you will be very helpful. If you have a
recent version of Python installed then this function will work:

```bash
function urlencode {
    python -c "import urllib;import sys;print urllib.quote_plus(sys.argv[1])" "$1"
}
```

With this function defined, a `curl` command to call this service with a filter can be simplified to
something like this:

```
curl -s "http://by-tor:8888/secured/analyses?proxyToken=$(cas-ticket)&filter=$(urlencode '[{"field":"app_name","value":"cace"}]')" | python -mjson.tool
```

Here's an example using no parameters:

```
$ curl -s "http://by-tor:8888/analyses?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "analyses": [
        {
            "app_description": "Counts and summarizes the number of lines, words, and bytes in a target file",
            "app_disabled": true,
            "app_id": "c7f05682-23c8-4182-b9a2-e09650a5f49b",
            "app_name": "Word Count",
            "deleted": null,
            "description": "Testing some jobs, yo.",
            "enddate": "1415040070501",
            "id": "6821c5c2-45b1-4f02-92fd-9c02992be7cc",
            "name": "wc_10291330",
            "resultfolderid": "/iplant/home/snow-dog/analyses/wc_10291330-2014-11-03-18-37-04.1",
            "startdate": "1415039824186",
            "status": "Failed",
            "username": "snow-dog@iplantcollaborative.org",
            "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Word%20Count"
        },
        ...
    ],
    "timestamp": "1415050735387",
    "total": 19
}
```

Here's an example of a search with a limit of one result:

```
$ curl -s "http://by-tor:8888/analyses?proxyToken=$(cas-ticket)&limit=1" | python -mjson.tool
{
    "analyses": [
        {
            "app_description": "Counts and summarizes the number of lines, words, and bytes in a target file",
            "app_disabled": true,
            "app_id": "c7f05682-23c8-4182-b9a2-e09650a5f49b",
            "app_name": "Word Count",
            "deleted": null,
            "description": "Testing some jobs, yo.",
            "enddate": "1415040070501",
            "id": "6821c5c2-45b1-4f02-92fd-9c02992be7cc",
            "name": "wc_10291330",
            "resultfolderid": "/iplant/home/snow-dog/analyses/wc_10291330-2014-11-03-18-37-04.1",
            "startdate": "1415039824186",
            "status": "Failed",
            "username": "snow-dog@iplantcollaborative.org",
            "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Word%20Count"
        }
    ],
    "timestamp": "1415050829308",
    "total": 19
}
```

## Deleting a Job

*Secured Endpoint:* DELETE /analyses/{analysis-id}

This endpoint marks a job as deleted in the DE database. After the job is
deleted, it will no longer be displayed in the _Analyses_ window. Attempts to
delete non-existent jobs and jobs that are already marked as deleted are no-ops,
but warning messages will appear in the log files. An attempt to delete a job
that was launched by another user will result in a n error. Upon success, the
the response body for this endpoint is an empty JSON object.

## Deleting Multiple Jobs

*Secured Endpoint:* POST /analyses/shredder

This endpoint is similar to `DELETE /analyses/{analysis-id}` except that it
allows the caller to mark multiple jobs as deleted at once. The format of the
request body is as follows:

```json
{
    "analyses": [
        "job-id-1",
        "job-id-2",
        ...,
        "job-id-n"
    ]
}
```

The response body for this endpoint is an empty JSON object if the service
succeeds.

Here's an example:

```
$ curl -X PUT -sd '
{
    "analyses": [
        "36df5b7e-3614-4c17-a0f9-3ad83d26a132",
        "deadbeef-feed-dead-beef-feeddeadbeef"
    ]
}
' "http://by-tor:8888/analyses/shredder?proxyToken=$(cas-ticket)" | python -mjson.tool
{}
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
    "id": "21dada70-acc3-4967-9c3f-73133e56724a"
}
```

Here's an example of an unsuccessful service call:

```
$ curl -X DELETE -s "http://by-tor:8888/secured/stop-analysis/21dada70-acc3-4967-9c3f-73133e56724a?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "error_code": "ERR_BAD_REQUEST",
    "reason": "job, 21dada70-acc3-4967-9c3f-73133e56724a, is already completed or canceled"
}
```
