Listing the config for data-info
-----------------------------

__URL Path__: /secured/admin/config

__HTTP Method__: GET

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Response Body__:

	{
	    "data-info.app.environment-name": "de-2",
	    "data-info.app.listen-port": "31325",
	    <...>
	    "success": true
	}


You can get the general idea of the format for the response body from the above.


Status Check
------------

__URL Path__: /secured/admin/status

__HTTP Method__: GET

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Response Body__:

	{
	    "iRODS": true,
	    "jex": true,
	    "metadactyl": true,
	    "notificationagent": true,
	    "success": true
	}

This check only checks to see if the services are responding to HTTP requests. It does not prove that the services are completely functional or configured correctly.
