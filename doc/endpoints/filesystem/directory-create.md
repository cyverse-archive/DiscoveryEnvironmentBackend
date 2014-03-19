Directory Creation
------------------
__URL Path__: /secured/filesystem/directory/create

__HTTP Method__: POST

__Error Codes__: ERR_NOT_WRITEABLE, ERR_EXISTS, ERR_NOT_A_USER

__Request Body__:

	{
		"path" : "/tempZone/home/rods/test3"
	}

__Response Body__:

    {
       "path":"\/tempZone\/home\/rods\/test3",
       "status":"success"
    }

__Curl Command__:

    curl -H "Content-Type:application/json" -d '' http://127.0.0.1:3000/secured/filesystem/directory/create?proxyToken=notReal
