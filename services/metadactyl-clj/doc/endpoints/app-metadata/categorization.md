# Table of Contents

* [App Categorization Endpoints](#app-categorization-endpoints)
    * [Deleting Categories](#deleting-categories)
    * [Categorizing Analyses](#categorizing-analyses)
    * [Listing Analysis Categorizations](#listing-analysis-categorizations)
    * [Adding Analyses to Analysis Groups](#adding-analyses-to-analysis-groups)

# App Categorization Endpoints

App categorization is strongly related to app listing in that, when apps are
listed, they're often listed by category. The endpoints related specifically to
categorizations are included in this document, but we've kept the app listing
endpoints in the [App Metadata Listing Services](listing.md) document.

## Deleting Categories

*Unsecured Endpoint:* POST /delete-categories

Analysis categories can be deleted using the `/delete-categories` entpoint.
This service accepts a list of analysis category identifiers and deletes all
corresponding analysis categories.  The request body is in the following
format:

```json
{
    "category_ids": [
        "category-id-1",
        "category-id-2",
        ...
        "category-id-n"
    ]
}
```

The response contains a list of category ids for which the deletion failed in
the following format:

```json
{
    "failures": [
        "category-id-1",
        "category-id-2",
        ...
        "category-id-n"
    ]
}
```

Here's an example:

```
$ curl -sd '
{
    "category_ids": [
        "D901F356-D33E-4AE9-8F92-0A07CE9AD70E"
    ]
}
' http://by-tor:8888/delete-categories | python -mjson.tool
{
    "failures": []
}
```

## Categorizing Analyses

*Unsecured Endpoint:* POST /categorize-analyses

When services are exported and re-imported, the analysis categorization
information also needs to be exported and re-imported. This service allows the
categorization information to be imported. Strictly speaking, this service can
also be used to move analyses to new categories, but this service hasn't been
used for that purpose since Belphegor and Conrad were created. This service is
documented in detail in the Analysis Categorization Services section of the
[tool integration services wiki page](https://pods.iplantcollaborative.org/wiki/display/coresw/Tool+Integration+Services).

The request body for this service is in this format:

```json
{
    "categories": [
        {
            "category_path": {
                "path": [
                    "root-category-name",
                    "first-subcategory-name",
                    ...,
                    "nth-subcategory-name"
                ],
                "username": "username"
            }
            "analysis": {
                "name": "analysis-name",
                "id": "analysis-id"
            }
        },
        ...
    ]
}
```

The response body format is identical to the request body format except that
only failed categorizations are listed and each categorization contains the
reason for the categorization failure. Here's the format:

```json
{
    "failed_categorizations": [
        {
            "reason": reason-for-failure,
            "category_path": {
                "path": [
                    root-category-name,
                    first-subcategory-name,
                    ...,
                    nth-subcategory-name
                ],
                "username": username
            }
            "analysis": {
                "name": analysis-name,
                "id": analysis-id
            }
        },
        ...
    ]
}
```

Here's an example:

```
$ curl -sd '
{
    "categories": [
        {
            "analysis": {
                "id": "Foo",
                "name": "Foo"
            },
            "category_path": {
                "username": "nobody@iplantcollaborative.org",
                "path": [
                    "Public Apps",
                    "Foo"
                ]
            }
        }
    ]
}
' http://by-tor:8888/categorize-analyses | python -mjson.tool
{
    "failed_categorizations": [
        {
            "categorization": {
                "analysis": {
                    "id": "Foo",
                    "name": "Foo"
                },
                "category_path": {
                    "path": [
                        "Public Apps",
                        "Foo"
                    ],
                    "username": "nobody@iplantcollaborative.org"
                }
            },
            "reason": "analysis Foo not found"
        }
    ]
}
```

## Listing Analysis Categorizations

*Unsecured Endpoint:* GET /get-analysis-categories/{category-set}

This is the counterpart to the /categorize-analyses endpoint; it loads
categorizations from the database and produces output in the format required by
the /categorize-analyes endpoint. The response body is in this format:

```json
{
    "categories": [
        {
            "category_path": {
                "path": [
                    "root-category-name",
                    "first-subcategory-name",
                    ...,
                    "nth-subcategory-name"
                ],
                "username": "username"
            }
            "analysis": {
                "name": "analysis-name",
                "id": "analysis-id"
            }
        },
        ...
    ]
}
```

This service can export the categorizations for two different sets of analyses
as described in the following table:

<table>
    <tr><th>Category Set</th><th>Description</th></tr>
    <tr><td>all</td><td>All analysis categorizations</td></tr>
    <tr><td>public</td><td>Only public analysis categorizations</td></tr>
</table>

Note that when only public analysis categorizations are exported, private
categorizations for public analyses are not included in the service output. This
means that if an analysis happens to be both in the user's private workspace and
in a public workspace then only the categorization in the public workspace will
be included in the output from this service. Here's an example:

```
$ curl -s http://by-tor:8888/get-analysis-categories/public | python -mjson.tool
{
    "categories": [
        {
            "analysis": {
                "id": "839E7AFA-031E-4DB8-82A6-AEBD56E9E0B9",
                "name": "hariolf-test-12"
            },
            "category_path": {
                "path": [
                    "Public Apps",
                    "Beta"
                ],
                "username": "<public>"
            }
        },
        ...
    ]
}
```

## Adding Analyses to Analysis Groups

*Unsecured Endpoint:* POST /add-analysis-to-group

Users in the Discovery Environment can add analyses to an analysis groups in
some cases. The most common use case for this feature is when the user wants to
add an existing analysis to his or her favorites. The request body for this
service is in this format:

```json
{
    "analysis_id": "analysis-id",
    "groups": [
        "group-id-1",
        "group-id-2",
        ...,
        "group-id-n"
    ]
}
```

If the service succeeds then the response body is an empty JSON object. Here's
an example:

```
$ curl -sd '
{
    "analysis_id": "9BCCE2D3-8372-4BA5-A0CE-96E513B2693C",
    "groups": [
        "028fce65-2504-4497-a20c-45e3cf8583b8"
    ]
}
' http://by-tor:8888/add-analysis-to-group | python -mjson.tool
{}
```
