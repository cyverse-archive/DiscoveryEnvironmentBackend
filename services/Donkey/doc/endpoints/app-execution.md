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

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

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

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

## Listing Jobs

*Secured Endpoint:* GET /analyses

This service forwards all requests to metadactyl. Please see the metadactyl
documentation for more details.

## Deleting a Job

*Secured Endpoint:* DELETE /analyses/{analysis-id}

This service forwards all requests to metadactyl. Please see the metadactyl
documentation for more details.

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

This endpoint forwards all requests to metadactyl. Please see the metadactyl
documentation for details.

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
