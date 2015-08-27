# Table of Contents

* [Collaborator List Management Endpoints](#collaborator-list-management-endpoints)
    * [Listing Collaborators](#listing-collaborators)
    * [Adding Collaborators](#adding-collaborators)
    * [Removing Collaborators](#removing-collaborators)
    * [Searching for Users](#searching-for-users)
    * [Obtaining User Info](#obtaining-user-info)

# Collaborator List Management Endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Listing Collaborators

Secured Endpoint: GET /secured/collaborators

This service delegates all of its calls to metadactyl's GET /collaborators
endpoint. Please refer to metadactyl's documentation for more information.

## Adding Collaborators

Secured Endpoint: POST /secured/collaborators

This service delegates all of its calls to metadactyl's POST /collaborators
endpoint. Please refer to metadactyl's documentation for more information.

## Removing Collaborators

Secured Endpoint: POST /secured/remove-collaborators

This service delegates all of its calls to metadactyl's POST
/collaborators/shredder endpoint. Please refer to metadactyl's documentation
for more information.

## Searching for Users

Secured Endpoint: GET /secured/user-search?search={search-string}

This endpoint allows the caller to search for user information by username,
email address and actual name. The search search string provided in the URL
should be URL encoded before being sent to the service. The response body is in
the following format:

```json
{
    "truncated": true|false,
    "users": [
      username-1: {
          "email": "email-address-1",
          "firstname": "first-name-1",
          "institution": "institution-1",
          "lastname": "last-name-1",
          "username": "username-1"
      },
      username-n: {
          "email": "email-address-n",
          "firstname": "first-name-n",
          "institution": "institution-n",
          "lastname": "last-name-n",
          "username": "username-n"
      }]
}
```

Assuming an error doesn't occur, the status code will be 200 and the response
body will contain up to the first fifty users whose username matched the search
string, up to the first fifty users whose actual name matched the search string,
and up to the first fifty users whose email address matched the search
string. Here's an example:

```
$ curl -s "http://by-tor:8888/secured/user-search?proxyToken=$(cas-ticket)&search=nobody" | python -mjson.tool
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

This endpoint delegates to iplant-groups' GET /subjects endpoint, but reformats the results to match the prior implementation with Trellis.

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

This endpoint delegates to iplant-groups' GET /subjects/:subject-id endpoint, but reformats the results to match the prior implementation with Trellis.
