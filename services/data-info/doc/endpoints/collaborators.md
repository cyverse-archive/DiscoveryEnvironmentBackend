# Table of Contents

* [Collaborator List Management Endpoints](#collaborator-list-management-endpoints)
    * [Listing Collaborators](#listing-collaborators)
    * [Adding Collaborators](#adding-collaborators)
    * [Removing Collaborators](#removing-collaborators)
    * [Searching for Users](#searching-for-users)
    * [Obtaining User Info](#obtaining-user-info)

# Collaborator List Management Endpoints

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](data-info-v-metadactyl.md) for more
information.

## Listing Collaborators

Secured Endpoint: GET /secured/collaborators

This service can be used to retrieve the list of collaborators for the
authenticated user. The response body is in the following format:

```json
{
    "success": true,
    "users": [
        {
            "email": "email-1",
            "firstname": "firstname-1",
            "id": "id-1",
            "lastname": "lastname-1",
            "useranme": "username-1"
        }
    ]
}
```

Here's an example:

```
$ curl -s "http://by-tor:8888/secured/collaborators?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true,
    "users": [
        {
            "email": "foo@iplantcollaborative.org",
            "firstname": "The",
            "id": 123,
            "lastname": "Foo",
            "username": "foo"
        },
        {
            "email": "bar@iplantcollaborative.org",
            "firstname": "The",
            "id": 456,
            "lastname": "Bar",
            "username": "bar"
        }
    ]
}
```

## Adding Collaborators

Secured Endpoint: POST /secured/collaborators

This service can be used to add users to the list of collaborators for the
current user. The request body is in the following format:

```json
{
    "users": [
        {
            "email": "email-1",
            "firstname": "firstname-1",
            "id": "id-1",
            "lastname": "lastname-1",
            "username": "username-1"
        }
    ]
}
```

Note that the only field that is actually required for each user is the
`username` field. The rest of the fields may be included if desired,
however. This feature is provided as a convenience to the caller, who may be
forwarding results from the user search service to this service.

An attempt to add a user that is already listed as a collaborator to the list of
collaborators will be silently ignored.

Here's an example:

```
$ curl -sd '
{
    "users": [
        {
            "username": "baz"
        }
    ]
}
' "http://by-tor:8888/secured/collaborators?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true
}
```

## Removing Collaborators

Secured Endpoint: POST /secured/remove-collaborators

This service can be used to remove users from the list of collaborators for the
current user. The request body is in the following format:

```json
{
    "users": [
        {
            "email": "email-1",
            "firstname": "firstname-1",
            "id": "id-1",
            "lastname": "lastname-1",
            "username": "username-1"
        }
    ]
}
```

Note that the only field that is actually required for each user is the
`username` field. The rest of the fields may be included if desired,
however. This feature is provided as a convenience to the caller, who may be
forwarding results from the user search service to this service.

An attempt to remove a user who is not listed as a collaborator from the list of
collaborators will be silently ignored.

Here's an example:

```
$ curl -sd '
{
    "users": [
        {
            "username": "baz"
        }
    ]
}
' "http://by-tor:8888/secured/remove-collaborators?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "success": true
}
```

## Searching for Users

Secured Endpoint: GET /secured/user-search/{search-string}

This endpoint allows the caller to search for user information by username,
email address and actual name. The search search string provided in the URL
should be URL encoded before being sent to the service. The response body is in
the following format:

```json
{
    username-1: {
        "email": "email-address-1",
        "firstname": "first-name-1",
        "id": "id-1",
        "institution": "institution-1",
        "lastname": "last-name-1",
        "position": "position-1",
        "username": "username-1"
    },
    username-n: {
        "email": "email-address-n",
        "firstname": "first-name-n",
        "id": "id-n",
        "institution": "institution-n",
        "lastname": "last-name-n",
        "position": "position-n",
        "username": "username-n"
    }
}
```

Assuming an error doesn't occur, the status code will be 200 and the response
body will contain up to the first fifty users whose username matched the search
string, up to the first fifty users whose actual name matched the search string,
and up to the first fifty users whose email address matched the search
string. Here's an example:

```
$ curl -s "http://by-tor:8888/secured/user-search/nobody?proxyToken=$(cas-ticket)" | python -mjson.tool
{
    "truncated": false,
    "users": [
        {
            "email": "nobody@iplantcollaborative.org,
            "firstname": Nobody",
            "id": "725",
            "institution": null,
            "lastname": "Atall",
            "position": null,
            "username": "nobody"
        }
    ]
}
```

## Obtaining User Info

Secured Endpoint: GET /secured/user-info

This endpoint allows the caller to search for information about users with
specific usernames. Each username is specified using the `username` query string
parameter, which can be specified multiple times to search for information about
more than one user. The response body is in the following format:

```json
{
    username-1: {
        "email": "email-address-1",
        "firstname": "first-name-1",
        "id": "id-1",
        "institution": "institution-1",
        "lastname": "last-name-1",
        "position": "position-1",
        "username": "username-1"
    },
    username-n: {
        "email": "email-address-n",
        "firstname": "first-name-n",
        "id": "id-n",
        "institution": "institution-n",
        "lastname": "last-name-n",
        "position": "position-n",
        "username": "username-n"
    }
}
```

Assuming the service doesn't encounter an error, the status code will be 200 and
the response body will contain the information for all of the users who were
found. If none of the users were found then the response body will consist of an
empty JSON object.

Here's an example with a match:

```
$ curl -s "http://by-tor:8888/secured/user-info?proxyToken=$(cas-ticket)&username=nobody" | python -mjson.tool
{
    "nobody": {
        "email": "nobody@iplantcollaborative.org",
        "firstname": "Nobody",
        "id": "3618",
        "institution": "iplant collaborative",
        "lastname": "Inparticular",
        "position": null,
        "username": "nobody"
    }
}
```

Here's an example with no matches:

```
$ curl -s "http://by-tor:8888/secured/user-info?proxyToken=$(cas-ticket)&username=foo" | python -mjson.tool
{}
```
