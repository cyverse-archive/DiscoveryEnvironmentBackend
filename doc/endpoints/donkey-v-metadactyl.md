# Table of Contents

* [Overview](#overview)
    * [Calling Secured Donkey Endpoints](#calling-secured-donkey-endpoints)
    * [Calling "Secured" Metadactyl Endpoints](#calling-secured-metadactyl-endpoints)
    * [A Simple Example](#a-simple-example)

# Overview

The "secured" endpoints in metadactyl behave a little bit differently from the
secured endpoints in Donkey. Specifically, the secured endpoints in Donkey
actually require user authentication whereas those in metadactyl merely require
information about the user making the request.

## Calling Secured Donkey Endpoints

All secured endpoints in Donkey require a query string parameter, `proxyToken`,
whose value contains a CAS service ticket that Donkey can then use to verify
that the user has authenticated with CAS. This query string parameter must be
provided in addition to all of the other query string parameters required by the
service itself.

The utility that many DE developers use to generate CAS service tickets is a
command-line utility called `cas-ticket`, which is referenced in several
examples included in the Donkey documentation. You'll often see it embedded in
the URL in curl commands like this:

```
$ curl "http://somehost/path/to/endpoint?proxyToken=$(cas-ticket)"
```

This is simple command substitution in Bash. Bash calls the `cas-ticket` script
and inserts the output in place of `$(cas-ticket)`. The substitutions occur a
little bit differently in the regression test suites used by the Quality
Assurance deparment, but the idea is the same.

## Calling "Secured" Metadactyl Endpoints

None of these endpoints are actually secured. They retain the "secured" label
because they're fronted by endpoints in Donkey that _are_ secure. That is, the
user interface doesn't hit the metadactyl services directly. Instead, it sends
the request to a Donkey endpoint that forwards the request to a corresponding
endpoint in metadactyl. The initial purpose of this separation was to provide
scalability and separation of concerns.

All secured endpoints in metadactyl support four query-string parameters
containing user attributes:

* `user` - the username
* `email` - the user's email address
* `first-name` - the user's first name
* `last-name` - the user's last name

Not all of these parameters are required by every secured service in metadactyl;
the only parameter that is required by all secured services is `user`. The rest
of the parameters are only required when the information in them is specifically
required. Donkey always passes all of these parameters to metadactyl when it
forwards requests.

## A Simple Example

One of the simplest endpoints in both Donkey and metadactyl is the bootstrap
endpoint, which is used to initialize the user's workspace in the DE. This
service requires one query-string parameter aside from the four listed above,
`ip-address`, which contains the IP address of the user's machine. This
parameter allows the service to record the source of the user's last session in
order to help users determine when their accounts may have been compromized.

The call to the Donkey service would look like this:

```
$ curl "http://by-tor:8888/secured/bootstrap?proxyToken=$(cas-ticket)&ip-address=127.0.0.1"
```

The equivalent call to the metadactyl service would look like this:

```
$ curl "http://by-tor:9999/secured/bootstrap?user=nobody&email=nobody@iplantcollaborative.org&first-name=Nobody&last-name=Inparticular&ip-address=127.0.0.1"
```
