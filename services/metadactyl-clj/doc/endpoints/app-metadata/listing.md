# Table of Contents

* [App Metadata Listing Services](#app-metadata-listing-services)
    * [Listing Data Objects in an Analysis](#listing-data-objects-in-an-analysis)
    * [Listing Deployed Components in an Analysis](#listing-deployed-components-in-an-analysis)

# App Metadata Listing Services

## Listing Data Objects in an Analysis

*Unsecured Endpoint:* GET /apps/{app-id}/data-objects

When a pipeline is being created, the UI needs to know what types of files are
consumed by and what types of files are produced by each analysis in the
pipeline. This service provides that information. The response body contains the
analysis identifier, the analysis name, a list of inputs (types of files
consumed by the service) and a list of outputs (types of files produced by the
service). The response format is:

```json
{
    "id": "analysis-id",
    "inputs": [
        {
            "data_object": {
                "cmdSwitch": "command-line-switch",
                "description": "description",
                "file_info_type": "info-type-name",
                "format": "data-format-name",
                "id": "data-object-id",
                "multiplicity": "multiplicity-name",
                "name": "data-object-name",
                "required": "required-data-object-flag",
                "retain": "retain-file-flag",
            },
            "description": "property-description",
            "id": "property-id",
            "isVisible": "visibility-flag",
            "label": "property-label",
            "name": "property-name",
            "type": "Input",
            "value": "default-property-value"
        },
        ...
    ]
    "name": analysis-name,
    "outputs": [
        {
            "data_object": {
                "cmdSwitch": "command-line-switch",
                "description": "description",
                "file_info_type": "info-type-name",
                "format": "data-format-name",
                "id": "data-object-id",
                "multiplicity": "multiplicity-name",
                "name": "data-object-name",
                "required": "required-data-object-flag",
                "retain": "retain-file-flag",
            },
            "description": "property-description",
            "id": "property-id",
            "isVisible": "visibility-flag",
            "label": "property-label",
            "name": "property-name",
            "type": "Output",
            "value": "default-property-value"
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -s http://by-tor:8888/analysis-data-objects/19F78CC1-7E14-481B-9D80-85EBCCBFFCAF | python -mjson.tool
{
    "id": "19F78CC1-7E14-481B-9D80-85EBCCBFFCAF",
    "inputs": [
        {
            "data_object": {
                "cmdSwitch": "",
                "description": "",
                "file_info_type": "File",
                "format": "Unspecified",
                "id": "A6210636-E3EC-4CD3-97B4-CAD15CAC0913",
                "multiplicity": "One",
                "name": "Input File",
                "order": 1,
                "required": true,
                "retain": false
            },
            "description": "",
            "id": "A6210636-E3EC-4CD3-97B4-CAD15CAC0913",
            "isVisible": true,
            "label": "Input File",
            "name": "",
            "type": "Input",
            "value": ""
        }
    ],
    "name": "Jills Extract First Line",
    "outputs": [
        {
            "data_object": {
                "cmdSwitch": "",
                "description": "",
                "file_info_type": "File",
                "format": "Unspecified",
                "id": "FE5ACC01-0B31-4611-B81E-26E532B459E3",
                "multiplicity": "One",
                "name": "head_output.txt",
                "order": 3,
                "required": true,
                "retain": true
            },
            "description": "",
            "id": "FE5ACC01-0B31-4611-B81E-26E532B459E3",
            "isVisible": false,
            "label": "head_output.txt",
            "name": "",
            "type": "Output",
            "value": ""
        }
    ]
}
```

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
