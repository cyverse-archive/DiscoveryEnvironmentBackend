# Security

None of the services in metadactyl are literally secured. Instead, services in
Donkey provide the security and forward requests to metadactyl, which should not
be accessible from outside the local network. In general, when a secured service
in Donkey forwards requests to metadactyl, the metadactyl services are marked as
secured so that the relationship between the Donkey and metadactyl services is
clear.

Generally speaking, the metadactyl services that correspond to secured services
in Donkey require user credentials, which are passed to the service in query
parameters. The available query string parameters are:

<table>
    <thead>
        <tr>
            <th>Name</th>
            <th>Description</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>user</td>
            <td>
                The short version of the username. That is, the username without
                the domain name included. For example, if the full username is
                `ipctest@iplantcollaborative.org` then the value of this
                parameter would simply be `ipctest`.
            </td>
        </tr>
        <tr>
            <td>email</td>
            <td>
                The user's full email address. This may or may not be the same
                as the full username of the user. For exmaple, it's possible to
                have a username of `nobody@iplantcollaborative.org` and an email
                address of `nobody-inparticular@example.org`. This parameter is
                not required for all secured services, but it is required for
                several of them.
            </td>
        </tr>
        <tr>
            <td>first-name</td>
            <td>
                The user's first name. At this time, this parameter is only
                required for the secured endpoints that update apps or pipelines
                This includes the following endpoints: `/secured/update-app`,
                `/secured/copy-app`, `/secured/copy-workflow`, and
                `/secured/make-analysis-public`.
            </td>
        </tr>
        <tr>
            <td>last-name</td>
            <td>
                The user's last name. This parameter is required by the same
                endpoints as `first-name`.
            </td>
        </tr>
        <tr>
            <td>ip-address</td>
            <td>
                The IP address of the originating request. Because these
                services are called by the DE on behalf of the user it is not
                possible to obtain the IP address of the originating request
                from the socket connection. This field is currently only
                required for the `/secured/bootstrap` and `/secured/logout`
                endpoints.
            </td>
        </tr>
    </tbody>
</thead>

Secured services can be distinguished from unsecured services by looking at the
path in the URL. The paths for all secured endpoints begin with `/secured`
whereas the paths for all other endpoints do not. In the documentation below,
services that are not secured will be labeled as unsecured endpoints and
services that are secured will be labeled as secured endpoints.

If the username is not provided in the query string of a request to a service
that requires it then an HTTP 401 (unauthorized) status will result, and there
will be no response body, even if the service normally has a response body.
