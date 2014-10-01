# Security

Several services in data-info require user authentication, which is managed by CAS
service tickets that are passed to the service in the `proxyToken` query
parameter. For example, the first service that the Discovery Environment hits
when a user logs in is the bootstrap service, which does require user
authentication. This service can be accessed using the URL,
`/bootstrap?proxyToken={some-service-ticket}&...` where {some-service-ticket}
refers to a service ticket string that has been obtained from CAS.

Secured services can be distinguished from unsecured services by looking at the
path in the URL. The paths for all secured endpoints begin with `/secured`
whereas the paths for all other endpoints do not. In the documentation below,
services that are not secured will be labeled as unsecured endpoints and
services that are secured will be labeled as secured endpoints.

If authentication or authorization fails for a secured service then an HTTP 401
(unauthorized) status will result, and there will be no response body, even if
the service normally has a response body.

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](endpoints/data-info-v-metadactyl.md)
for more information.
