# Table of Contents

* [Reference Genome Endpoints](#reference-genome-endpoints)
    * [Exporting Reference Genomes](#exporting-reference-genomes)
    * [Importing Reference Genomes](#importing-reference-genomes)

# Reference Genome Endpoints

## Exporting Reference Genomes

*Secured Endpoint:* GET /secured/reference-genomes

This service can be used to export reference genomes from the discovery
environment, presumably in order to import them into another deployment of the
discovery environment.

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/reference-genomes?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "genomes": [
        {
            "created_by": "<public>",
            "created_on": 1345848226895,
            "deleted": false,
            "last_modified_by": "<public>",
            "last_modified_on": "",
            "name": "Arabidopsis lyrata (Ensembl 14)",
            "path": "/path/to/Arabidopsis_lyrata.1.0/de_support/",
            "uuid": "4bb9856a-43da-4f67-bdf9-f90916b4c11f"
        },
        ...
    ],
    success: true
}
```

## Importing Reference Genomes

*Secured Endpoint:* PUT /secured/reference-genomes

This service can be used to import reference genomes into the discovery
environment. The request body for this service should be in the same format as
the response body for the endpoint to export the reference genomes. Note that
the success flag, if present in the request body, will be ignored. This endpoint
replaces *ALL* of the reference genomes in the discovery environment, so if a
genome is not listed in the body of this request, it will not show up in the DE.

Here's an example:

```
$ curl -X PUT -sd '
{
    "genomes": [
        {
            "created_by": "<public>",
            "created_on": 1345848226895,
            "deleted": false,
            "last_modified_by": "<public>",
            "last_modified_on": "",
            "name": "Arabidopsis lyrata (Ensembl 14)",
            "path": "/path/to/Arabidopsis_lyrata.1.0/de_support/",
            "uuid": "4bb9856a-43da-4f67-bdf9-f90916b4c11f"
        }
    ]
}
' "http://by-tor:8888/secured/reference-genomes?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "success": true
}
```
