Listing the config for Donkey
-----------------------------

__URL Path__: /secured/admin/config

__HTTP Method__: GET

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Response Body__:

	{
	    "donkey.app.environment-name": "de-2",
	    "donkey.app.listen-port": "31325",
	    <...>
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
	    "notificationagent": true
	}

This check only checks to see if the services are responding to HTTP requests. It does not prove that the services are completely functional or configured correctly.
