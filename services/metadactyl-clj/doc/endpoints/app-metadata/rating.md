# Table of Contents

* [App Rating Services](#app-rating-services)
    * [Rating Analyses](#rating-analyses)
    * [Deleting Analysis Ratings](#deleting-analysis-ratings)

# App Rating Services

## Rating Analyses

*Secured Endpoint:* POST /secured/rate-analysis

Users have the ability to rate an analysis for its usefulness, and this service
provides the means to store the analysis rating. This service accepts an
analysis identifier a rating level between one and five, inclusive, and a
comment identifier that refers to a comment in iPlant's Confluence wiki. The
rating is stored in the database and associated with the authenticated user. The
request body for this service is in the following format:

```json
{
    "analysis_id": "analysis-id",
    "rating": "selected-rating",
    "comment_id": "comment-identifier"
}
```

The response body for this service contains only the average rating for the
analysis, and is in this format:

```json
{
    "avg": "average-rating",
}
```

Here's an example:

```
$ curl -sd '
{
    "analysis_id": "72AA400D-6945-463E-A18D-09513C2381D7",
    "rating": 4,
    "comment_id": 27
}
' "http://by-tor:8888/secured/rate-analysis?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "avg": 4
}
```

## Deleting Analysis Ratings

*Secured Endpoint:* POST /secured/delete-rating

The DE uses this service to remove a rating that a user has previously made.
This service accepts an analysis identifier in a JSON request body and deletes
the authenticated user's rating for the corresponding analysis. The request body
for this service is in the following format:

```json
{
    "analysis_id": "analysis-id",
}
```

The response body for this service contains only the new average rating for the
analysis and is in the following format:

```json
{
    "avg": "average-rating",
}
```

Here's an example:

```
$ curl -sd '
{
    "analysis_id": "a65fa62bcebc0418cbb947485a63b30cd"
}
' "http://by-tor:8888/secured/delete-rating?user=snow-dog&email=sd@example.org" | python -mjson.tool
{
    "avg": 0
}
```
