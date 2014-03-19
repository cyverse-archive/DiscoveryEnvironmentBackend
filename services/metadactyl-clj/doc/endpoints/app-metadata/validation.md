# Table of Contents

* [App Validation Endoints](#app-validation-endoints)
    * [Validating Analyses for Pipelines](#validating-analyses-for-pipelines)
    * [Determining if an Analysis Can be Exported](#determining-if-an-analysis-can-be-exported)
    * [Determining if an Analysis Can be Made Public](#determining-if-an-analysis-can-be-made-public)

# App Validation Endoints

## Validating Analyses for Pipelines

*Unsecured Endpoint:* GET /validate-analysis-for-pipelines/{analysis-id}

Multistep analyses and empty analyses can't currently be included in pipelines,
so the UI needs a way to determine whether or not an analysis can be included in
a pipeline. This service provides that information. The response body contains a
flag indicating whether or not the analysis can be included in a pipeline along
with the reason. If the analysis can be included in a pipeline then the reason
string will be empty. The response format is:

```json
{
    "is_valid": "flag",
    "reason", "reason"
}
```

Here are some examples:

```
$ curl -s http://by-tor:8888/validate-analysis-for-pipelines/9A39F7FA-4025-40E2-A720-489FA93C6A93 | python -mjson.tool
{
    "is_valid": true,
    "reason": ""
}
```

```
$ curl -s http://by-tor:8888/validate-analysis-for-pipelines/BDB011B6-1F6B-443E-B94E-400930619978 | python -mjson.tool
{
    "is_valid": false,
    "reason": "analysis, BDB011B6-1F6B-443E-B94E-400930619978, has too many steps for a pipeline"
}
```

## Determining if an Analysis Can be Exported

*Unsecured Endpoint:* POST /can-export-analysis

Some analyses can't be exported to Tito because they contain no steps, contain
multiple steps or contain types of properties that have been deprecated and are
no longer supported in Tito. The UI uses this service to determine whether or
not an analysis can be exported to Tito before attempting to do so. The request
body for this service is in this format:

```json
{
    "analysis_id": "analysis-id"
}
```

If the analysis can be exported then the response body will be in this format:

```json
{
    "can-export": true
}
```

If the analysis can't be exported then the response body will be in this
format:

```json
{
    "can-export": false,
    "cause": "reason"
}
```

Here are some examples:

```
$ curl -sd '{"analysis_id": "BDB011B6-1F6B-443E-B94E-400930619978"}' http://by-tor:8888/can-export-analysis | python -mjson.tool
{
    "can-export": false,
    "cause": "Application contains Properties that cannot be copied or modified at this time.."
}
```

```
$ curl -sd '{"analysis_id": "19F78CC1-7E14-481B-9D80-85EBCCBFFCAF"}' http://by-tor:8888/can-export-analysis | python -mjson.tool
{
    "can-export": true
}
```

## Determining if an Analysis Can be Made Public

*Secured Endpoint:* GET /is-publishable/{analysis-id}

A multi-step analysis can't be made public if any of the apps that are included
in the analysis are not public. This endpoint returns a true flag if the app is
a single-step app or it's a multistep app in which all of the apps included in
the pipeline are public. The response body is in the following format:

```json
{
    "action": "is-publishable",
    "publishable": true,
    "status": "success"
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/is-publishable/3EDD7D2E-51DC-48AC-9084-802B973B563A?user=nobody" | python -mjson.tool
{
    "action": "is-publishable",
    "publishable": false,
    "status": "success"
}
```
