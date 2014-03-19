Object State Management
=======================

Provides HTTP JSON API on top of MongoDB. Allows a subset of MongoDB operations
on a document and provides a callback mechanism that can be used to notify other
services that a document has changed.


Building the OSM
----------------

You'll need leiningen installed. Then run the following commands:

```
lein deps
lein ring uberwar
```

Installing the OSM
------------------

Assuming that the OSM is running on the same host as MongoDB, this should be as
simple as dropping the WAR file into your servlet container.


Verifying the OSM is working
----------------------------

We can check that the OSM is alive:

```
curl http://127.0.0.1:3000/jobs/blah
```

or

```
curl http://127.0.0.1:3000/jobs/foobarbazbang
```

We can check the log too:

```
tail -f /opt/tomcat/logs.log
```

This just shows the service is responding, the result should be:

```
That url doesn't exist.
```


Creating documents / creating "objects"
---------------------------------------

```
curl -sd '{"foo":"bar"}' http://127.0.0.1:3000/jobs
```

This will produce a UUID:

```
32060E54-F92A-1358-33EC-8E6F22BBBACD
```

This identifier will be used with subsequent updates.  The above value returned
will varied since it's a generated identifier.

We can grab that the freshly created document with a simple HTTP GET:

```
curl http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD | python -mjson.tool
{
    "object_persistence_uuid": "32060E54-F92A-1358-33EC-8E6F22BBBACD",
    "state": {
        "foo": "bar"
    },
    "history": [

    ],
    "callbacks": [

    ]
}
```


Updating documents / updating "objects"
---------------------------------------

We can update the document (or, object state) like so:

```
curl -sd '
{
    "foo": "bar",
    "status": "RUNNING",
    "whatever": "theheck",
    "jerry": "wants"
}
' http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD | python -mjson.tool
{
    "object_persistence_uuid": "32060E54-F92A-1358-33EC-8E6F22BBBACD",
    "state": {
        "foo": "bar",
        "status": "RUNNING",
        "whatever": "theheck",
        "jerry": "wants"
    },
    "history": [
        {
            "foo": "bar",
            "status": "RUNNING"
        }
    ],
    "callbacks": [

    ]
}
```

Keep in mind that this is replacing the document that is stored.

Grab the UUID and check it to see if your changes made it:

```
curl http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD
```

The previous state is available in ``"history"``.

When state changes, there are callbacks that will fire an HTTP PORT to the URL
defined by the callback's ``callback`` value: check the callbacks:

```
curl http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD/callbacks | python -mjson.tool
{
    "callbacks": []
}
```

To test that callbacks are firing, you may wish to create a
"[post-bin](http://www.postbin.org/)".  This is a service that will create a
some bucket for the OSM to HTTP POST data into and allow you to view it.  Visit
[PostBin](http://www.postbin.org/) to create your data bucket.  You can monitor
changes with an Atom syndication feed, or by hitting the URL given when you
create it.  It will allow you to inspect the content and the headers of the HTTP
POSTs.  (github has a short
[description](http://help.github.com/testing-webhooks/) of using PostBin)

When a document is updated, so any update, the OSM will do an HTTP POST to a
callback of ``"type"`` "on_update" to the URL defined in ``"callback"``.  This
means that any modification to the document (even if the same state was posted)
will cause a callback to be fired.  If you want more refinement, you'd use
"on_change."  When a document has an actual state change, the OSM will do an
HTTP POST to a callback of ``"type"`` "on_change" to the URL defined in
``"callback"``.

For clarifications on the callback events, please refer to the OSM
[documentation](https://pods.iplantcollaborative.org/wiki/display/coresw/Object+State+Management+System)
for more detailed information.


Add callbacks:
--------------

```
curl -sd '
{
    "callbacks": [
        {
            "callback": "http://www.postbin.org/r51w6z",
            "type": "on_change"
        },
        {
            "callback": "http://www.google.com/",
            "type": "on_update"
        }
    ]
}
' http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD/callbacks | python -mjson.tool
{
    "callbacks": [
        {
            "callback": "http://www.postbin.org/r51w6z",
            "type": "on_change"
        },
        {
            "callback": "http://www.google.com",
            "type": "on_update"
        }
    ]
}
```

The results returned will be a list of the current callbacks on the object.  If
you want to verify that result then just do the following:

```
curl http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD/callbacks | python -mjson.tool
{
    "callbacks": [
        {
            "callback": "http://www.postbin.org/r51w6z",
            "type": "on_change"
        },
        {
            "callback": "http://www.google.com/",
            "type": "on_update"
        }
    ]
}
```

You can add callbacks for two "event" types (or callback event types):
"on_update" and "on_change"

Please refer to the OSM
[documentation](https://pods.iplantcollaborative.org/wiki/display/coresw/Object+State+Management+System)
for more detailed information.


Deleting callbacks
------------------

```
curl -sd '
{
    "callbacks": [
        {
            "callback": "http://www.google.com/",
            "type": "on_update"
        }
    ]
}
' http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD/callbacks/delete | python -mjson.tool
{
    "callbacks": [
        {
            "callback": "http://www.postbin.org/r51w6z",
            "type": "on_change"
        }
    ]
}
```

The response to the delete command will be the remaining callbacks on the
object.

Verify they have been deleted:

```
curl http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD/callbacks
{
    "callbacks":[]
}
```


Querying for what state you're after
------------------------------------

Let's update the OSM with some more "realistic" data:

The input JSON is:

```
{
    "uuid": "multistep3-89fb-4d70-0650-0xC0FFEE",
    "name": "job1",
    "user": "ana",
    "workspace_id": "1",
    "dag_id": "323",
    "submission_date": "Sun Dec 19 2010 12:50:38 GMT-0700 (MST)",
    "status": "Submitted",
    "foo": "baz",
    "whatever": "theheck",
    "jerry": "wants"
}
```

(Note: You may wish to save this to a file and include it with curl via the
response-file interface (putting the @ in front of the filename))

```
curl -sd '
{
    "dag_id": "323",
    "foo": "baz",
    "jerry": "wants",
    "name": "job1",
    "status": "Submitted",
    "submission_date": "Sun Dec 19 2010 12:50:38 GMT-0700 (MST)",
    "user": "ana",
    "uuid": "multistep3-89fb-4d70-0650-0xC0FFEE",
    "whatever": "theheck",
    "workspace_id": "1"
}
' http://127.0.0.1:3000/jobs/32060E54-F92A-1358-33EC-8E6F22BBBACD | python -mjson.tool
```

This will respond with the new contents and the state management values:

```
{
    "object_persistence_uuid": "32060E54-F92A-1358-33EC-8E6F22BBBACD",
    "state": {
        "uuid": "multistep3-89fb-4d70-0650-0xC0FFEE",
        "name": "job1",
        "user": "ana",
        "workspace_id": "1",
        "dag_id": "323",
        "submission_date": "Sun Dec 19 2010 12:50:38 GMT-0700 (MST)",
        "status": "Submitted",
        "foo": "baz",
        "whatever": "theheck",
        "jerry": "wants"
    },
    "history": [
        {
            "foo": "baz",
            "status": "HELD",
            "whatever": "theheck",
            "jerry": "wants"
        }
    ],
    "callbacks": [
        {
            "type": "on_change",
            "callback": "http://www.postbin.org/r51w6z"
        }
    ]
}
```

Now let's query for the ``"state.uuid"``.  We put the query in the POST body:

```
curl -sd '
{
    "state.uuid" : "multistep3-89fb-4d70-0650-0xC0FFEE"
}
' "http://127.0.0.1:3000/jobs/query" | python -mjson.tool
{
    "objects": [
        {
            "object_persistence_uuid": "32060E54-F92A-1358-33EC-8E6F22BBBACD",
            "state": {
                "uuid": "multistep3-89fb-4d70-0650-0xC0FFEE",
                "name": "job1",
                "user": "ana",
                "workspace_id": "1",
                "dag_id": "323",
                "submission_date": "Sun Dec 19 2010 12:50:38 GMT-0700 (MST)",
                "status": "Submitted",
                "foo": "baz",
                "whatever": "theheck",
                "jerry": "wants"
            },
            "history": [
                {
                    "foo": "baz",
                    "status": "HELD",
                    "whatever": "theheck",
                    "jerry": "wants"
                }
            ],
            "callbacks": [
                {
                    "type": "on_change",
                    "callback": "http://www.postbin.org/r51w6z"
                }
            ]
        }
    ]
}
```

This should help prove that Object State Management (OSM) is functioning as
expected.

Counting objects that match a query
-----------------------------------

If you want to perform a search in the OSM, but just obtain the number of
documents in the OSM that match your query then you can use the `count`
service.  The request body for this service is identical to that of the query
service, but the response body contains only the message count.  Continuing with
the example from the previous section, you can count the number of documents
with the UUID mentioned above like this:

```
curl -sd '
{
    "state.uuid" : "multistep3-89fb-4d70-0650-0xC0FFEE"
}
' "http://127.0.0.1:3000/jobs/count" | python -mjson.tool
{
    "count": 1
}
```

Similarly, you can query for all documents that are associated with the user,
`ana`, by doing this:

```
curl -sd '
{
    "state.user" : "ana"
}
' "http://127.0.0.1:3000/jobs/count" | python -mjson.tool
{
    "count": 1
}
```

For more information on the query format, please see the [mongoDB
documentation](http://www.mongodb.org/display/DOCS/Querying).
