# iDrop Lite Cart

    /cart

This method creates a new iDrop Lite cart.

## Request

### Method

    POST

### Parameters

Parameter | Required? | Description
--------- | --------- | -----------
user      | yes       | the iRODS username of the owner of the cart
folder    | no        | an absolute path to the folder whose members will be added to the cart

### Body

If provided, the request body should contain a list of files and folders to add to the shopping
cart.

#### Required?

No.

#### Content Type

    application/json

#### Schema

    PathList {
      paths (array): an array of absolute paths
    }

## Response

Only the responses with custom bodies are documented.

### Created (201)

The cart was created.

#### Content Type

    application/json

#### Schema

    Cart {
      key (string):                    the unique id of the cart
      user (string):                   the username of the owner of the cart
      zone (string):                   the authentication zone for the user
      host (string):                   the FQDN of the iRODS host
      port (number):                   the IP port iRODS listens to
      defaultStorageResource (string): the default storage resource of the user
      home (string):                   the user's home folder
      password (string):               a one use password for external access to the cart
    }

### Bad Request (400)

The `user` parameter wasn't provided.

#### Content Type

    application/json

#### Schema

    MissingQueryParameters {
      error_code (string}: "ERR_MISSING_QUERY_PARAMETER",
      parameters (array):  an array of the missing parameter names
    }

### Unprocessable Entity (422)

One of several problems can result in this response.

* The user doesn't exist.
* The folder provided in the URL doesn't exist.
* A JSON request body was provided, but it wasn't well formed.

#### Content Type

    application/json

#### Schema

    UnprocessableCartResponse {
      error_code (string): an error code indicating the cause,
      fields (array):      an array containing "paths", if the request body wasn't well formed.
      path (string):       the value of the folder query parameter,
      user (string):       the value of the user query parameter
    }

## Example

    ? curl localhost:31360/cart?user=tedgin | python -mjson.tool
    {
        "defaultStorageResource": "",
        "home": "/iplant/home/tedgin",
        "host": "irods-2.iplantcollaborative.org",
        "key": "1415297480997",
        "password": "20a3ab291b29b287eb329fabaee75294",
        "port": 1247,
        "user": "tedgin",
        "zone": "iplant"
    }
