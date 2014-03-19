File/Directory existence
------------------------
The /exists endpoint allows the caller to check for the existence of a set of files. The following is an example call to the exists endpoint:

__URL Path__: /secured/filesystem/exists

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

	{
		"paths" : [
			"/iplant/home/wregglej/pom.xml",
			"/iplant/home/wregglej/pom.xml2"
		]
	}

__Response Body__:

    {
        "action":"exists",
        "status":"success",
        "paths":{
            "/iplant/home/wregglej/pom.xml2":false,
            "/iplant/home/wregglej/pom.xml":false
        }
    }

__Curl Command__:

    curl -H "Content-type:application/json" -d '{"paths" : ["/iplant/home/wregglej/pom.xml", "/iplant/home/wregglej/pom.xml2"]}' 'http://127.0.0.1:3000/secured/filesystem/exists?proxyToken=notReal'
