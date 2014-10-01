Creating Tickets
----------------
__URL Path__: /secured/filesystem/tickets

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE

__Request Parameters__:
* proxyToken - A valid CAS ticket.

* public - Tells data-info whether to make the ticket accessible to the public group.
           Setting it to 1 means that the ticket will be made publicly accessible. Any other value
           means that the ticket will not be accessible publicly. This parameter is optional and
           defaults to not making tickets public.

__Request Body__:

    {
        "paths" : [
            "/path/to/file/or/directory",
            "/path/to/another/file/or/directory"
        ]
    }

__Response Body__:

    {
        "action" : "add-tickets",
        "user" : "<username>",
        "tickets" : [
            {
                "path"              : "/path/to/file/or/directory",
                "ticket-id"         : "<ticket-id>",
                "download-url"      : "http://127.0.0.1:8080/d/<ticket-id>",
                "download-page-url" : "http://127.0.0.1:8080/<ticket-id>"
            }
        ]
    }

__Curl Command__:

    curl -d '{"paths":"/path/to/file/or/directory","/path/to/another/file/or/directory"]}' 'http://127.0.0.1:3000/secured/filesystem/tickets?proxyToken=notReal&public=1'


Listing Tickets
---------------
__URL Path__: /secured/filesystem/list-tickets

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE

__Request Parameters__:
* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths" : ["list of paths to look up tickets for"]
    }

__Response Body__:

    {
        "tickets" : {
            "/path/to/file" : [
                {
                    "path"              : "/path/to/file",
                    "ticket-id"         : "<ticket-id>",
                    "download-url"      : "http://127.0.0.1:8080/d/<ticket-id>",
                    "download-page-url" : "http://127.0.0.1:8080/<ticket-id>"
                }
            ],
            "/path/to/dir"  : [
                {
                    "path"              : "/path/to/dir",
                    "ticket-id"         : "<ticket-id>",
                    "download-url"      : "http://127.0.0.1:8080/d/<ticket-id>",
                    "download-page-url" : "http://127.0.0.1:8080/<ticket-id>"
                }
            ]
        }
    }

__Curl Command__:

    curl -d '{"paths":["/path/to/file","/path/to/dir"]}' http://127.0.0.1:3000/secured/filesystem/list-tickets?proxyToken=notReal


Deleting Tickets
----------------
__URL Path__: /secured/filesystem/delete-tickets

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_DOES_NOT_EXIST, ERR_NOT_WRITEABLE

__Request Parameters__:
* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "tickets" : ["ticket-id1", "ticket-id2"]
    }

__Response Body__:

    {
        "tickets" : ["ticket-id1", "ticket-id2"]
    }

__Curl Command__:

    curl -d '{"tickets":["ticket-id1","ticket-id2"]}' http://127.0.0.1:4000/secured/filesystem/delete-tickets?proxyToken=notReal
