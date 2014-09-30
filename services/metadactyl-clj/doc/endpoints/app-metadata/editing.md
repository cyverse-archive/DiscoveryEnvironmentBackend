# Table of Contents

* [App Editing Services](#app-editing-services)
    * [Making a Copy of an Analysis Available for Editing in Tito](#making-a-copy-of-an-analysis-available-for-editing-in-tito)
    * [Submitting an Analysis for Public Use](#submitting-an-analysis-for-public-use)

# App Editing Services

## Making a Copy of an Analysis Available for Editing in Tito

*Secured Endpoint:* GET /secured/copy-template/{analysis-id}

This service can be used to make a copy of an analysis in the user's workspace.
The response body consists of a JSON object containing the ID of the new
analysis.

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/copy-template/C720C42D-531A-164B-38CC-D2D6A337C5A5?user=snow-dog&email=sd@example.org" | python -m json.tool
{
    "analysis_id": "13FF6D0C-F6F7-4ACE-A6C7-635A17826383"
}
```

## Submitting an Analysis for Public Use

*Secured Endpoint:* POST /secured/make-analysis-public

This service can be used to submit a private analysis for public use. The user
supplies basic information about the analysis and a suggested location for it.
The service records the information and suggested location then places the
analysis in the Beta category. A Tito administrator can subsequently move the
analysis to the suggested location at a later time if it proves to be useful.
The request body is in the following format:

```json
{
    "analysis_id": "analysis-id",
    "references": [
        "reference-link-1",
        "reference-link-2",
        ...,
        "reference-link-n"
    ],
    "groups": [
        "suggested-group-1",
        "suggested-group-2",
        ...,
        "suggested-group-n"
    ],
    "desc": "analysis-description",
    "name": "analysis-name",
    "wiki_url": "documentation-link"
}
```

The response body is just an empty JSON object if the service call succeeds.

Making an analysis public entails recording the additional inforamtion provided
to the service, removing the analysis from all of its current analysis groups,
adding the analysis to the _Beta_ group.

The `desc` and `name` fields are both optional. If either field is not provided
in the request body then the current name or description will be retained.

Here's an example:

```
$ curl -sd '
{
    "analysis_id": "F771A215-4809-4683-87C0-A899C0732AF3",
    "references": [
        "http://foo.bar.baz.org"
    ],
    "groups": [
        "0A687324-099B-4EEF-A82C-C1A60B970487"
    ],
    "desc": "The foo is in the bar.",
    "name": "Where's the foo, again?",
    "wiki_url": "https://wiki.iplantcollaborative.org/docs/Foo+Foo"
}
' "http://by-tor:8888/secured/make-analysis-public?user=snow-dog&email=sd@example.org&first-name=Snow&last-name=Dog"
{}
```
