# Table of Contents

* [App Metadata Listing Services](#app-metadata-listing-services)
    * [Listing Deployed Components in an Analysis](#listing-deployed-components-in-an-analysis)

# App Metadata Listing Services

## Listing Deployed Components in an Analysis

*Secured Endpoint:* GET /secured/get-components-in-analysis/{analysis-id}

This service lists information for all of the deployed components that are
associated with an analysis. This information used to be included in the results
of the analysis listing service. The response body is in the following format:

```json
{
    "deployed_components": [
        {
            "attribution": "attribution-1",
            "description": "description-1",
            "id": "id-1",
            "location": "location-1",
            "name": "name-1",
            "type": "type-1",
            "version": "version-1"
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/get-components-in-analysis/0BA04303-F0CB-4A34-BACE-7090F869B332?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "deployed_components": [
        {
            "attribution": "",
            "description": "",
            "id": "c73ef66158ef94f1bb90689ff813629f5",
            "location": "/usr/local2/muscle3.8.31",
            "name": "muscle",
            "type": "executable",
            "version": ""
        },
        {
            "attribution": "",
            "description": "",
            "id": "c2d79e93d83044a659b907764275248ef",
            "location": "/usr/local2/phyml-20110304",
            "name": "phyml",
            "type": "executable",
            "version": ""
        }
    ]
}
```
