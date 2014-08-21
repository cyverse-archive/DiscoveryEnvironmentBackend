JEX
===
Backend service that accepts JSON and submits DAGs to Condor with condor_submit_dag.


Configuration
-------------
The JEX is intended to be run as a user that can submit jobs to a Condor cluster. The condor_submit_dag executable needs to be on the PATH for the user that JEX runs as.

JEX reads in its configuration from a file. By default, it will look for
the file at /etc/iplant/de/jex.properties, but you can override the
path by passing JEX the --config setting at start up.


Input
-----
The JEX's "/" endpoint takes JSON in the following format (keep in mind this is simplistic):

    {
        "callback": "http://example.org/specified-callback",
        "execution_target" : "condor",
        "analysis_id" : "ac37ced41d82346f68d1b27b56830526c",
        "name" : "jex_example",
        "analysis_name" : "Word Count",
        "username" : "auser",
        "request_type" : "submit",
        "uuid" : "j3b297bae-f264-4c3e-b5a5-4e049d524754",
        "email" : "an.email@example.org",
        "workspace_id" : "5",
        "notify" : true,
        "output_dir" : "/path/to/irods/output-dir",
        "create_output_subdir" : true,
        "description" : "",
        "analysis_description" : "Counts the number of words, characters, and bytes in a file",
        "file-metadata" : [
            {
                "attr" : "example-attribute",
                "value" : "example-value",
                "unit" : "example-unit"
            }
        ],
        "steps" : [
            {
                "name" : "step_1",
                "type" : "condor",
                "environment" : {
                    "key" : "value"
                },
                "config" : {
                    "input" : [
                        {
                            "name" : "input",
                            "property" : "input",
                            "type" : "File",
                            "value" : "/path/to/irods/input",
                            "id" : "wcInput",
                            "multiplicity" : "single",
                            "retain" : false
                        }
                    ],
                    "params" : [
                        {
                            "name" : "",
                            "order" : 0,
                            "value" : "input",
                            "id" : "wcInput"
                        },
                        {
                            "name" : "",
                            "value" : "wc_out.txt",
                            "order" : 1,
                            "id" : "wcOutput"
                        }
                    ],
                    "output" : [
                        {
                            "name" : "wc_out.txt",
                            "property" : "wc_out.txt",
                            "type" : "File",
                            "multiplicity" : "single",
                            "retain" : true
                        },
                        {
                            "name" : "logs",
                            "property" : "logs",
                            "type" : "File",
                            "multiplicity" : "collection",
                            "retain" : true
                        }
                    ]
                },
                "component" : {
                    "name" : "wc_wrapper.sh",
                    "type" : "executable",
                    "description" : "Word Count",
                    "location" : "/usr/local3/bin/wc_tool-1.00"
                }
            }
        ]
    }

If the "callback" field is not present in the request JSON then a default callback URL will be used instead. The default callback URL is currently the `/job-status` endpoint in the notification agent.

If the "environment" field is present in a step map, then the key will be used as the environment variable and the value will be the value the environment variable is set to, wrapped in double-quotes. For example this map:

    {
        "PATH" : "/usr/local/bin:$PATH"
    }

Will result in the step it's associated with having 'PATH="/usr/local/bin:$PATH"' prepended to it in the resulting shell script. The change in the environment will only be in effect for the step the map is associated with.

An example curl will look like this:

    curl -H "Content-Type:application/json" -d 'Insert overly complicated JSON here' http://127.0.0.1:3000/

The result will look something like this:

    Analysis submitted.
    DAG ID: 1611
    OSM ID: 18CCA2F8-C08F-4E03-A4BC-9F25BE48FE5A

An error will result in a 500 HTTP error code and a stack-trace wrapped in JSON:

    {
        "message" : "Error message",
        "stack-trace" : "stacktrace here"
    }

An error can also result in JSON in the following format along with a 500 status code:

    {
        "error_code" : "<an error code>"
    }

Redirecting stdout and stderr
-----------------------------

Each step in the analysis can independently redirect stdout and stderr to files within the working directory. To do this, add the "stderr" and/or "stdout" fields to the step object. For example:

    {
        "name" : "step_1",
        "type" : "condor",
        "stdout" : "this_is_a_stdout_redirection",
        "stderr" : "this_is_a_stderr_redirection",
        "config" : {
            ...Removed for irrelevancy on this topic...
        },
        "component" : {
            ...Removed for irrelevancy on this topic...
        }
    }

The stdout and stderr fields should contain paths relative to the current working directory. Invalid paths will either result in stderr/stdout being lost or in an analysis execution failure. Since we don't have access to the execution nodes when jobs are submitted, the JEX cannot confirm that the paths listed in stderr/stdout are valid.

Previewing Arguments
--------------------

To get a preview of an argument list, do a HTTP POST against /arg-preview. The JSON POSTed to this URL should be in this format:

    {
        "params" : [
            {
                "name" : "-t",
                "value" : "foo",
                "order" : 2
            },
            {
                "name" : "-u",
                "value" : "bar",
                "order" : 1
            },
            {
                "name" : "-v",
                "value" : "baz",
                "order" : 0
            }
        ]
    }

Here's a sample curl command:

    curl -H "Content-Type:application/json" -d '{"params" : [{"name" : "-t", "value" : "foo", "order" : 2}, {"name" : "-u", "value" : "bar", "order" : 1}, {"name" : "-v", "value" : "baz", "order" : 0}]}' http://lame.example.com31330/arg-preview

Here's the successful response:

    {
        "status":"success",
        "action":"arg-preview",
        "params":"-v 'baz' -u 'bar' -t 'foo'"
    }

Unsuccessful responses will return a HTTP 500 status code and JSON containing an error code. The general format of the error response is:

    {
        "error_code" : "<an error code>"
    }

The error JSON may contain additional keys, but don't write code against them. Here are the known error codes that may get returned:

    * ERR_UNCHECKED_EXCEPTION
    * ERR_INVALID_JSON

Stopping a running analysis
---------------------------

To stop an executing analysis, do a HTTP DELETE against the /stop/:uuid endpoint. Substitute an analysis uuid for the :uuid in the path. Here's an example with curl:

    curl -X DELETE http://services-2.iplantcollaborative.org:31330/stop/07248d40-c707-11e1-9b21-0800200c9a66

You should get JSON in the body of a 200 HTTP response formatted as follows:

    {
        "action" : "stop",
        "status" : "success",
        "condor-id" : "<A Condor identifier>"
    }

On an error, you should get a 500 HTTP response with a JSON body formatted as follows when the condor_rm command returns a non-zero status:

    {
        "action" : "stop",
        "status" : "failure",
        "error_code" : "ERR_FAILED_NON_ZERO",
        "sub_id" : "<The Condor submission id>",
        "err" : "<stderr from the condor_rm command>",
        "out" : "<stdout from the condor_rm command>"
    }

Or, if the UUID can't be found in the OSM:

   {
       "action" : "stop",
       "status" : "failure",
       "error_code" : "ERR_MISSING_CONDOR_ID",
       "uuid" : "<the uuid passed in>"
   }

Or for more general errors:

    {
        "action" : "stop",
        "status" : "failure",
        "error_code" : "ERR_UNCHECKED_EXCEPTION",
        "message" : "<error specific message>"
    }

An explanation of the individual fields in the input JSON, how they interact, and what is added by the JEX is forthcoming.
