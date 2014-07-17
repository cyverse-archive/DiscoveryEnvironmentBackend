# Table of Contents

* [App Execution Endpoints](#app-execution-endpoints)
    * [Submitting a Job for Execution](#submitting-a-job-for-execution)

# App Execution Endpoints

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
