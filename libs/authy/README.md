# authy

A Clojure library designed to provide simple support for OAuth authentication.

## Usage

This library provides a simple set of functions that can be used to authenticate to an OAuth 2.0
server. It currently provides functions to get an access token for an authorization code and to
obtain a new access token for the current token, assuming that a refresh token is associated with
the current access token.

### Defining OAuth Server Parameters

The server information is a map of connection details:

```clojure
(def server-info
 {:token-uri      "https://oauth-server.example.org/oauth/token"
  :redirect-uri   "https://oauth-client.example.org/path/to/auth/redirect"
  :client-key     "some-fake-client-identifier"
  :client-secret  "some-fake-client-passcode"
  :token-callback (fn [token-info] (do-something-with token-info))})
```

The fields are defined as follows:

<table border="1">
    <thead>
        <tr><th>Field</th><th>Definition</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>token-uri</td>
            <td>The URI of the endpoint used to obtain access tokens</td>
        </tr>
        <tr>
            <td>redirect-uri</td>
            <td>The redirect URI sent in the authorization request.</td>
        </tr>
        <tr>
            <td>client-key</td>
            <td>The API key used to identify the client.</td>
        </tr>
        <tr>
            <td>client-secret</td>
            <td>The API secret used to identify the client.</td>
        </tr>
        <tr>
            <td>token-callback</td>
            <td>A function that will called when a new token is obtained.</td>
        </tr>
    </tbody>
</table>

The callback function is intended to be used by the calling service to do something when a new
access token is obtained. For example, the caller may wish to cache the token so that it can be
reused in future requests. This is helpful in cases where a client library automatically handles
retries for expired tokens, preventing the caller from having to handle retries while still
allowing the token information to be stored.

### Obtaining an Access Token from an Authorization Code

When an authorization code is received, the receiver can obtain an access token by calling
`get-access-token`:

```clojure
(def token-info (get-access-token server-info authorization-code))
```

The resulting map contains both the token information and the server information, which keeps all
of the information required to obtain a refresh token in one place. In addition to the server
information fields, the response contains the following information about the token:

<table border="1">
    <thead>
        <tr><th>Field</th><th>Definition</th></tr>
    </thead>
    <tbody>
        <tr>
            <td>token-type</td>
            <td>The type of the access token.</td>
        </tr>
        <tr>
            <td>expires-at</td>
            <td>The approximate time the token expires (java.util.Date).</td>
        </tr>
        <tr>
            <td>refresh-token</td>
            <td>A token that can be used to obtain a new access token.</td>
        </tr>
        <tr>
            <td>access-token</td>
            <td>The access token itself.</td>
        </tr>
    </tbody>
</table>

### Refreshing an Access Token

When an access token that has a refresh token associated with it expires, a new token can be
obtained by calling `refresh-access-token`:

```clojure
(def new-token-info (refresh-access-token token-info))
```

The resulting map is in the same format as the return value of `get-access-token`.

### Determining if an Access Token is Expired

You can determine if an access token is expired by calling `token-expired?`:

```clojure
(def expired? (token-expired? token-info))
```

## License

http://iplantcollaborative.org/sites/default/files/iPLANT-LICENSE.txt
