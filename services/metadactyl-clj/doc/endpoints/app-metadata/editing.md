# Table of Contents

* [App Editing Services](#app-editing-services)
    * [Making a Copy of an Analysis Available for Editing in Tito](#making-a-copy-of-an-analysis-available-for-editing-in-tito)
    * [Submitting an Analysis for Public Use](#submitting-an-analysis-for-public-use)
    * [Making a Copy of a Pipeline Available for Editing](#making-a-copy-of-a-pipeline-available-for-editing)

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

## Making a Copy of a Pipeline Available for Editing

*Secured Endpoint:* GET /secured/copy-workflow/{analysis-id}

This service can be used to make a copy of a Pipeline in the user's workspace.
Note that both the "user" and "email" authentication parameters are required for
this endpoint in order to properly set the Pipeline copy's integrator details.
This endpoint will copy the Analysis details, steps, and mappings, but will not
copy templates used in the Pipeline steps.
The response body contains the JSON representation of the new Pipeline copy in
the same format provided by the
[edit-workflow endpoint](#making-a-pipeline-available-for-editing).

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/copy-workflow/2751855E-BFA5-472D-B34D-1D079CA9DDE6?user=snow-dog&email=sd@example.org" | python -m json.tool
{
    "analyses": [
        {
            "analysis_id": "502EFC27-3106-4181-A5C4-BD260ECD57AF",
            "analysis_name": "Copy of ...",
            "description": "...",
            "mappings": [
                {
                    "source_step": "step_1_...",
                    "target_step": "step_2_...",
                    "map": {
                        "outputID...": "inputID..."
                    }
                }
            ],
            "steps": [
                {
                    "app_type": "...",
                    "template_id": "...",
                    "description": "...",
                    "name": "step_1_..."
                },
                {
                    "app_type": "...",
                    "template_id": "...",
                    "description": "...",
                    "name": "step_2_..."
                }
            ]
        }
    ],
    "templates": [
        {
            "outputs": [
                {
                    "format": "Unspecified",
                    "required": true,
                    "description": "",
                    "name": "...",
                    "id": "outputID..."
                }
            ],
            "inputs": [
                {
                    "format": "Unspecified",
                    "required": true,
                    "description": "",
                    "name": "...",
                    "id": "..."
                }
            ],
            "description": "...",
            "name": "...",
            "id": "..."
        },
        {
            "outputs": [
                {
                    "format": "Unspecified",
                    "required": true,
                    "description": "",
                    "name": "...",
                    "id": "..."
                }
            ],
            "inputs": [
                {
                    "format": "Unspecified",
                    "required": true,
                    "description": "",
                    "name": "...",
                    "id": "inputID..."
                }
            ],
            "description": "...",
            "name": "...",
            "id": "..."
        }
    ]
}
```
