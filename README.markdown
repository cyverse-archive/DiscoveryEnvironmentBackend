# Conrad

Conrad is a REST-like HTTP API for administering apps in the Discovery
Environment.  These are the back-end services for Belphegor, the app
administration console for the Discovery Environment.

## Installing and Configuring Conrad

Conrad is packaged as an RPM and published in iPlant's YUM repositories.  It
can be installed using `yum install conrad` and upgraded using `yum upgrade
conrad`.

### Primary Configuration

Conrad gets most of its configuration settings from Apache Zookeeper.  These
configuration settings are uploaded to Zookeeper using Clavin, a command-line
tool maintained by iPlant that allows configuration properties and access
control lists to be easily uploaded to Zookeeper.  Please see the Clavin
documentation for information about how to upload configuration settings.
Here's an example Conrad configuraiton file:

```properties
# The listen port.
conrad.listen-port=65535

# The database vendor ("postgresql" or "mysql")
conrad.db.vendor=postgresql

# The database connection settings.
conrad.db.host=localhost
conrad.db.port=5432
conrad.db.name=de
conrad.db.user=user
conrad.db.password=password

# Expire connections after the specified number of minutes.
conrad.db.max-idle-minutes=180

# The settings to use for proxy authentication.
conrad.cas.server=https://cas-server-hostname/cas/
conrad.server-name=https://local-hostname

# The user domain name setting
conrad.uid-domain=iplantcollaborative.org
```

Generally, the only database settings that will have to be changed are the
database connection settings.  Since the discovery environment currently
always uses PostgreSQL, the vendor should never have to be changed.  The rest
of the database settings are fairly self-explanatory.

The CAS settings deserve some explanation.  `conrad.cas.server` refers to the
base URL of the CAS server.  This URL will be used when the services are
validating CAS proxy tickets.  conrad.server-namne refers to the name on the
server on which conrad is deployed.  Typically, this should be an HTTPS URL,
but HTTP URLs are acceptable for testing when signed certificates are not
available.

### Zookeeper Connection Information

One piece of information that can't be stored in Zookeeper is the information
required to connect to Zookeeper.  For Conrad, this is stored in a single file:
`/etc/conrad/conrad.properties`.  Here's an example:

```properties
zookeeper=zookeeper://127.0.0.1:2181
```

After installing Conrad, it will be necessary to modify this file so that it
points to the correct host and port.

### Logging Configuration

The logging settings are stored in WEB-INF/classes/log4j.properties inside the
WAR file.  By default, the file will look like this:

```properties
log4j.rootLogger=WARN, A

log4j.appender.B=org.apache.log4j.ConsoleAppender
log4j.appender.B.layout=org.apache.log4j.PatternLayout
log4j.appender.B.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n

log4j.appender.A=org.apache.log4j.RollingFileAppender
log4j.appender.A.File=/var/log/conrad/conrad.log
log4j.appender.A.layout=org.apache.log4j.PatternLayout
log4j.appender.A.layout.ConversionPattern=%d{MM-dd@HH:mm:ss} %-5p (%13F:%L) %3x - %m%n
log4j.appender.A.MaxFileSize=10MB
log4j.appender.A.MaxBackupIndex=1
```

See [the log4j documentation](http://logging.apache.org/log4j/1.2/manual.html)
for logging configuration instructions.

## Services

All URLs referenced below are listed as relative URLs with value names enclosed
in braces.  For example, the service to retrieve the list of applications in a
category is accessed using the URL, /get-apps-in-group/{group-id}, where
{group-id} refers to the UUID used to identify the application group.  On the
other hand, all examples use fully qualified URLs.

All request and response bodies are in JSON format.  To avoid confusion between
the braces used to denote JSON objects and braces used to denote example values,
example values in JSON bodies are not enclosed in braces, but instead listed as
hyphen-separated-names without enclosing quotes.

The terms, "group" and "category," both refer to an application category and are
treated synonymously in the text below.

### Verifying that Conrad is Running

Endpoint: GET /

The root path in Conrad can be used to verify that Conrad is actually running.
Currently, the response to this URL contains only a welcome message.  The
response may be enhanced to include usage instructions or status information at
some time in the future if it proves to be helpful.  Here's an example:

    dennis$ curl -s http://by-tor:14444/
    Welcome to Conrad!

### Listing App Groups

Endpoint: GET /secured/get-app-groups

The /get-app-groups endpoint is used to obtain a hierarchical list of public app
categories.  The output from this service is very similar to the output from
Donkey's /secured/app-groups endpoint.

Here's an example:

```
dennis$ curl -s http://by-tor:14444/secured/get-app-groups | python -mjson.tool
{
    "groups": [
        {
            "description": "",
            "groups": [
                {
                    "description": "",
                    "groups": [],
                    "id": "g5401bd146c144470aedd57b47ea1b979",
                    "is_public": true,
                    "name": "Beta",
                    "template_count": 8
                },
                ...
            ],
            "id": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
            "is_public": true,
            "name": "Public Applications",
            "template_count": 82
        }
    ]
}
```

Note that the list of app groups will contain a special group whose name and
identifier are both set to _Trash_.  The Trash group represents a
pseudo-category that contains all deleted and orphaned apps.  (Orphaned apps are
apps that aren't in any category and thus aren't displayed in the Discovery
Environment whether or not they're marked as deleted.)

### Listing Apps in a Category

Endpoint: GET /secured/get-apps-in-group/{group-id}

The /get-apps-in-group/{group-id} endpoint is used to obtain a list of apps in
an analysis group.  The output from this service is very similar to the output
from the Discovery Environment's /get-analyses-in-group/{group-id} endpoint.
Here's an example:

```
dennis$ curl -s http://by-tor:14444/secured/get-apps-in-group/EAD6C97D-8D7D-4199-B15E-6B1DABAB2D5F | python -mjson.tool
{
    "description": "",
    "hid": 258,
    "id": "EAD6C97D-8D7D-4199-B15E-6B1DABAB2D5F",
    "is_public": true,
    "name": "Bar",
    "template_count": 1,
    "templates": [
        {
            "disabled": false,
            "deleted": false,
            "description": "c...and t",
            "id": "D36D47B0-A82F-40AB-AB1F-037249944620",
            "integrator_email": "snowdog@iplantcollaborative.org",
            "integrator_name": "Snow Dog",
            "integration_date": 1341265753338,
            "is_favorite": false,
            "is_public": true,
            "name": "c and t",
            "pipeline_eligibility": {
                "is_valid": true,
                "reason": ""
            },
            "rating": {
                "average": 0.0
            },
            "suggested_categories": [
                {
                    "description": "",
                    "id": "EAD6C97D-8D7D-4199-B15E-6B1DABAB2D5F",
                    "name": "Bar",
                    "workspace_id": 0
                }
            ],
            "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/CACE"
        }
    ],
    "workspace_id": 0
}
```

### Listing Deployed Components in an App

Endpoint: GET /secured/get-components-in-app/{app-id}

This service lists all of the deployed components that are used in the app
with the given identifier.  Here are some examples:

```
$ curl -s http://by-tor:14444/secured/get-components-in-app/81B7457C-3758-41AB-AEEC-6D9D3A0CD585 | python -mjson.tool
{
    "deployed_components": [
        {
            "attribution": "",
            "description": "Wrapper script to drive BEDtools in the iPlant DE",
            "id": "c9610d44f56064e39809656f0cdfe833c",
            "location": "/usr/local3/bin/bedtools-iplant-1.00",
            "name": "bedtools.pl",
            "type": "executable",
            "version": "1.00"
        }
    ]
}
```

This service will fail under the following circumstances:

* the application with the specified identifier can't be found;
* a database error occurs.

### Updating an App

Endpoint: POST /secured/update-app

Much of the information included in the app listing can be updated using the
/update-app endpoint.  The post body of this service contains the app ID along
with the new values to be included in the app listing:

```json
{
    "id": application-id,
    "name": new-application-name,
    "description": new-application-description,
    "integration_date": new-integration-date,
    "wiki_url": new-documentation-url,
    "disabled": disabled-flag,
    "integrator_name": new-integrator-name,
    "integrator_email": new-integrator-email-address
}
```

Note that all fields are required; all fields that are omitted will be set to
null in the database.  This is acceptable for now because this service is only
accessed by the Belphegor UI.  If this service is ever made available to end
users then this behavior will have to be changed.

Upon success, the response body contains a success flag along with a complete
listing of the updated app.  Upon failure, the response body contains a success
flag along with a brief description of the reason for the failure.

Here are some examples:

```
dennis$ curl -sd '
{
    "id": "F50FE07D-91AA-AA36-1D47-BB2E7FDC7BB4",
    "name": "Scooby Snacks!",
    "description": "Reah, reah, reah, reah, reah!",
    "integration_date": 1327526294069,
    "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Rummy",
    "disabled": false,
    "integrator_name": "Scooby Dooby Doo",
    "integrator_email": "scooby@iplantcollaborative.org"
}
' http://by-tor:14444/secured/update-app | python -mjson.tool
{
    "application": {
        "deleted": false,
        "description": "Reah, reah, reah, reah, reah!",
        "disabled": false,
        "id": "F50FE07D-91AA-AA36-1D47-BB2E7FDC7BB4",
        "integrator_email": "scooby@iplantcollaborative.org",
        "integrator_name": "Scooby Dooby Doo",
        "integration_date": 1341265753338,
        "is_favorite": false,
        "is_public": true,
        "name": "Scooby Snacks!",
        "pipeline_eligibility": {
            "is_valid": true,
            "reason": ""
        },
        "rating": {
            "average": 0.0
        },
        "suggested_categories": [],
        "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Rummy"
    },
    "success": true
}

dennis$ curl -sd '
{
    "id": "Foo",
    "name": "Scooby Snacks!",
    "description": "Reah, reah, reah, reah, reah!",
    "integration_date": 1327526294069,
    "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Rummy",
    "disabled": false,
    "integrator_name": "Scooby Dooby Doo",
    "integrator_email": "scooby@iplantcollaborative.org"
}
' http://by-tor:14444/secured/update-app | python -mjson.tool
{
    "reason": "app, Foo, not found",
    "success": false
}
```

This service will fail under the following circumstances:

* the application with the specified identifier can't be found;
* a database error occurs.

Validation of the fields in the request body is not currenty being done.  If
these services are exposed to direct access by users at some point in the future
then validation will be added.

### Renaming a Category

Endpoint: POST /secured/rename-category

An application category can be renamed using the /rename-category endpoint.  The
request body for this service should contain a JSON object that specifies the
category identifier and the new category name:

```json
{
    "categoryId": category-id,
    "name": category-name
}
```

The response body is a JSON object containing a success flag along with the new
category name.

Here are some examples:

```
dennis$ curl -sd '
{
    "categoryId": "g5401bd146c144470aedd57b47ea1b979",
    "name": "Etabay"
}
' http://by-tor:14444/secured/rename-category | python -mjson.tool
{
    "name": "Etabay",
    "success": true
}

dennis$ curl -sd '
{
    "categoryId": "Foo",
    "name": "Etabay"
}
' http://by-tor:14444/secured/rename-category | python -mjson.tool
{
    "reason": "category, Foo, does not exist",
    "success": false
}
```

This service will fail under the following circumstances:

* the category ID is not specified in the request body;
* the new category name is not specified in the request body;
* the new category name is too long;
* the category with the specified identifier can't be found;
* any parent of the category already contains a subcategory with the new name;
* a database error occurs.

### Deleting a Category

Endpoint: DELETE /secured/category/{category-id}

An application category can be deleted by sending an HTTP DELETE request to the
/category/{category-id} endpoint.  This service takes no request body.  Upon
success, the response body contains a success flag along with the category
identifier.  Upon failure, the response body contains a success flag along with
a brief description of the reason for the failure.

Here are some examples:

```
dennis$ curl -sXDELETE http://by-tor:14444/secured/category/6FB48BDB-B034-48CE-8242-096525F50662 | format_json
{
   "success" : true,
   "categoryId" : "6FB48BDB-B034-48CE-8242-096525F50662"
}

dennis$ curl -sXDELETE http://by-tor:14444/secured/category/g5401bd146c144470aedd57b47ea1b979 | format_json
{
   "success" : false,
   "reason" : "category, g5401bd146c144470aedd57b47ea1b979, contains apps"
}
```

This service will fail under the following circumstances:

* a category with the specified identifier can't be found;
* the specified category contains apps;
* the specified category contains subgroups;
* a database error occurs.

### Creating a New Category

Endpoint PUT /secured/category

An application category can be created by sending an HTTP PUT request to the
/category endpoint.  The request body for this service should contain the
identifier of the parent category, the name of the new category, and an optional
description of the new category:

```json
{
    "parentCategoryId": parent-category-id,
    "name": new-category-name,
    "description": new-category-description
}
```

Upon success, the response body contains a success flag along with a complete
listing of the newly created category.  Upon failure, the response body contains
a success flag along with a brief description of the reason for the failure.
Here are some examples:

```
dennis$ curl -sXPUT -d '
{
    "parentCategoryId": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
    "name": "Foo"
}
' http://by-tor:14444/secured/category | python -mjson.tool
{
    "category": {
        "description": "",
        "hid": 244,
        "id": "FF7155FC-132B-4AAB-9FBB-57DC40DC572B",
        "is_public": true,
        "name": "Foo",
        "template_count": 0,
        "templates": [],
        "workspace_id": 0
    },
    "success": true
}

dennis$ curl -sXPUT -d '
{
    "parentCategoryId": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
    "name": "Foo"
}
' http://by-tor:14444/secured/category | python -mjson.tool
{
    "reason": "category, g12c7a585ec233352e31302e323112a7ccf18bfd7364, already contains a subcategory named, \"Foo\"",
    "success": false
}
```

This service will fail under the following circumstances:

* the parent category ID is not specified in the request body;
* the new category name is not specified in the request body;
* the new category name is too long;
* the parent category can't be found;
* the parent category directly contains apps;
* the parent category already contains a subcategory with the specified name;
* a database error occurs.

### Deleting an App

Endpoint: DELETE /secured/app/{app-id}

An app can be logically deleted by sending a DELETE request to the /app/{app-id}
endpoint.  This service takes no request body.  Upon success, this service
returns a success flag along with the identifier of the application that was
deleted.  Upon failure, this service returns a success flag along with a brief
description of the reason for the failure.

Here are some examples:

```
dennis$ curl -sXDELETE http://by-tor:14444/secured/app/2BB00471-502E-42DE-A57E-9B516CEA1493 | python -mjson.tool
{
    "id": "2BB00471-502E-42DE-A57E-9B516CEA1493",
    "success": true
}

dennis$ curl -sXDELETE http://by-tor:14444/secured/app/Foo | python -mjson.tool
{
    "reason": "app, Foo, not found",
    "success": false
}
```

This service will fail under the following circumstances:

* an app with the specified identifier can't be found;
* a database error occurs.

### Undeleting an App

Endpoint: GET /secured/undelete-app/{app-id}

An app that has been deleted can be logically undeleted by sending a GET
request to the /undelete-app/{app-id} endpoint.  This service takes no request
body.  Upon success, this service returns a success flag along with the
identifier of the application that was deleted and a list of the public
categories that the app is in.  Upon failure, this service returns a success
flag along with a brief description of the reason for the failure.

Here are some examples:

```
dennis$ curl -s http://by-tor:14444/secured/undelete-app/8E25FB08-E11E-476A-95A9-0D06563F261A | python -mjson.tool
{
    "categories": [
        {
            "description": null,
            "id": "7A0B68B2-6534-4CAD-8439-E0FAA3FD82B3",
            "name": "ChIPseq Analysis",
            "workspace_id": 0
        }
    ],
    "id": "8E25FB08-E11E-476A-95A9-0D06563F261A",
    "success": true
}

dennis$ curl -s http://by-tor:14444/secured/undelete-app/Foo | python -mjson.tool
{
    "reason": "app, Foo, not found",
    "success": false
}
```

This service will fail under the following circumstances:

* an app with the specified identifier can't be found;
* the speified app is not associated with an app category;
* a database error occurs.

### Moving an App

Endpoint: POST /secured/move-app

An app can be moved to a new category by sending an HTTP POST request to the
/move-app endpoint.  The request body should contain a JSON object with fields
containing the application ID and the identifier of the new category:

```json
{
    "id": app-id
    "categoryId": category-id
}
```

Upon success, this service returns a JSON object containing a success flag and a
complete listing of the new parent category.  Upon failure, this service returns
a JSON object containing a success flag and a brief description of the reason
for the failure.

Here are some examples:

```
dennis$ curl -sd '
{
    "id": "D36D47B0-A82F-40AB-AB1F-037249944620",
    "categoryId": "44EB59FA-E49E-480F-BBD4-7FCC91E3D1EB"
}
' http://by-tor:14444/secured/move-app | python -mjson.tool
{
    "category": {
        "description": "",
        "hid": 259,
        "id": "44EB59FA-E49E-480F-BBD4-7FCC91E3D1EB",
        "is_public": true,
        "name": "Baz",
        "template_count": 1,
        "templates": [
            {
                "disabled": false,
                "deleted": false,
                "description": "Reah, reah, reah, reah, reah!",
                "id": "D36D47B0-A82F-40AB-AB1F-037249944620",
                "integrator_email": "scooby@iplantcollaborative.org",
                "integrator_name": "Scooby Dooby Doo",
                "integration_date": 1341265753338,
                "is_favorite": false,
                "is_public": true,
                "name": "Scooby Snacks!",
                "pipeline_eligibility": {
                    "is_valid": true,
                    "reason": ""
                },
                "rating": {
                    "average": 0.0
                },
                "suggested_categories": [
                    {
                        "description": "",
                        "id": "EAD6C97D-8D7D-4199-B15E-6B1DABAB2D5F",
                        "name": "Bar",
                        "workspace_id": 0
                    }
                ],
                "wiki_url": "https://pods.iplantcollaborative.org/wiki/display/DEapps/Rummy"
            }
        ],
        "workspace_id": 0
    },
    "success": true
}

dennis$ curl -sd '
{
    "id": "Foo",
    "categoryId": "06DFCE72-BC04-4556-9659-B4D87A471947"
}
' http://by-tor:14444/secured/move-app | python -mjson.tool
{
    "reason": "app, Foo, not found",
    "success": false
}
```

As a special case, if the destination category identifier is set to the
special pseudocategory identifier, _Trash_, then the app will be marked as
deleted rather than moved.  Note that whenever an app is moved to a new
category other than _Trash_ the app is automatically marked as not deleted.
This feature allows clients to effectively delete and undelete apps by moving
them to or from the _Trash_ category, respectively.

This service will fail under the following circumstances:

* the application identifier is not specified in the request body;
* the destination category identifier is not specified in the request body;
* the specified application can't be found;
* the specified destination category can't be found;
* the specified destination category contains subcategories;
* a database error occurs.

### Moving a Category

Endpoint: POST /secured/move-category

A category can be moved to a new parent category by sending an HTTP POST request
to the /move-category endpoint.  The request body should contain a JSON object
containing the identifier of the category to move along with the identifier of
the new parent category:

```json
{
    "categoryId": category-id,
    "parentCategoryId": new-parent-category-id
}
```

Upon success, this service returns a success flag along with a complete category
listing.  Upon failure, this service returns a success flag along with a brief
description of the reason for the failure.

Here are some examples:

```
dennis$ curl -sd '
{
    "categoryId": "A8D08BAA-D930-4178-9647-2A17DB17E309",
    "parentCategoryId": "g12c7a585ec233352e31302e323112a7ccf18bfd7364"
}
' http://by-tor:14444/secured/move-category | python -mjson.tool
{
    "categories": {
        "groups": [
            {
                "description": "",
                "groups": [
                    {
                        "description": null,
                        "groups": [
                            {
                                "description": null,
                                "groups": [],
                                "id": "DE2F3D2B-1D0B-4A2D-BD6C-EBDE2F7E974A",
                                "is_public": true,
                                "name": "Aligners",
                                "template_count": 5
                            },
                        ]
                    },
                    ...
                ],
                "id": "g12c7a585ec233352e31302e323112a7ccf18bfd7364",
                "is_public": true,
                "name": "Public Applications",
                "template_count": 82
            }
        ]
    },
    "success": true
}

dennis$ curl -sd '
{
    "categoryId": "A8D08BAA-D930-4178-9647-2A17DB17E309",
    "parentCategoryId": "Foo"
}
' http://by-tor:14444/secured/move-category | python -mjson.tool
{
    "reason": "category, Foo, does not exist",
    "success": false
}
```

This service will fail under the following circumstances:

* the child category identifier is not specified in the request body;
* the parent category identifier is not specified in the request body;
* the child category can't be found;
* the parent category can't be found;
* the parent category contains apps;
* the parent category is a descendent of the child category;
* the parent categor already contains a subcategory of the same name;
* a database error occurs.

---

### Genome References

These services are endpoints that interface with the genome_references table of the Discovery Enviroment database. Genome references hold metadata about stored genomes.

The metadata they hold is namely a unique identifier of the reference called a uuid, the path where the genome file is held, the user id of the creator of the genome file, the name of the genome being referenced, whether or not the genome has been marked as deleted, when the reference was last modified, and who modified it last.

All the genome reference endpoint will return a success body with a JSON representation of the serviced data.

500 errors are usually caught so they are rare. They will happen most commonly when passing incorrectly formed JSON data when creating and modifying genome references.

#### Listing All Genome References

Let's say you want to get a list of all genome references in the database, there are two forms of endpoints that retrieve 'all' genome references.

1. GET **'/all-genome-references'** This will return ALL references including ones flagged as 'deleted'.

2. GET **'/genome-references'** This will return just the references that are not flagged as 'deleted'.

Imagine Conrad is running on localhost at port 3000, then a command requesting data for ALL genome references in the database will look like this:

```curl -v http://localhost:3000/secured/all-genome-references | python -mjson.tool```

The output of this command looks like this:

```
{
    "genomes": [
        {
            "created_by": "andyloveshockeyeh@iplantcollaborative.org,
            "created_on": "1338503205918",
            "deleted": false,
            "id": "20",
            "last_modified_by": "",
            "last_modified_on": "",
            "name": "Physcomitrella patens Phypa1.1 (Ensembl 13)",
            "path": "/data2/collections/genomeservices/0.2/Physcomitrella_patens.Phypa1.1/de_support/",
            "uuid": "9B5AED20-7882-44AB-BEBC-8DD7B4C7E13F"
        },
        {
            "created_by": "stormeschinrules@iplantcollaborative.org",
            "created_on": "1338503205918",
            "deleted": true,
            "id": "22",
            "last_modified_by": "",
            "last_modified_on": "",
            "name": "Rattus norvegicus RGSC3.4 (Ensembl 66)",
            "path": "/data2/collections/genomeservices/0.2/Rattus_norvegicus.RGSC3.4/de_support/",
            "uuid": "080C3A4A-3566-4C62-9F4D-04F129915761"
        }
    ],
    "success": true
}
```

Note how there are both `"deleted": false` and `"deleted": true` entries returned because we asked for `all-genome-references/` which even returns 'deleted' genome references.

There will probably be a LOT more data, try it yourself!


#### Listing Genome References by Username

Endpoint: GET **genome-references-by-user/{username}**

Returns all genome references created by the passed username, this skips deleted references however.

#####Example
If you want to search for genome references created_by the user "kobain@iplantcollaborative.org", the command looks like this:

```curl -v http://localhost:3000/secured/genome-references-by-user/kobain@iplantcollaborative.org | python -mjson.tool```

The output of this command:

```
{
    "genomes": [
        {
            "created_by": "kobain@iplantcollaborative.org,
            "created_on": "1338503205918",
            "deleted": false,
            "id": "02",
            "last_modified_by": "",
            "last_modified_on": "",
            "name": "Physcomitrella patens Phypa1.1 (Ensembl 13)",
            "path": "/data2/collections/genomeservices/0.2/Physcomitrella_patens.Phypa1.1/de_support/",
            "uuid": "9B5AED20-7882-44AB-BEBC-8DD7B4C7E13F"
        },
        {
            "created_by": "kobain@iplantcollaborative.org",
            "created_on": "1338503205918",
            "deleted": false,
            "id": "02",
            "last_modified_by": "",
            "last_modified_on": "",
            "name": "Rattus norvegicus RGSC3.4 (Ensembl 66)",
            "path": "/data2/collections/genomeservices/0.2/Rattus_norvegicus.RGSC3.4/de_support/",
            "uuid": "080C3A4A-3566-4C62-9F4D-04F129915761"
        }
    ],
    "success": true
}
```
**NOTE:** Usernames are CaSe sensitive!


#### Listing a Specific Genome by its UUID

Endpoint: GET **genome-reference/{uuid}**

This endpoint will return the genome reference specified by the passed Universal Unique ID. This will even return a reference marked as deleted.

#####Example
If you want to search for genome reference with the uuid 1CFEAAD6-F5F2-4B2A-BA85-A16A9E8EB35C, the command looks like this:

```curl -v http://localhost:3000/secured/genome-reference/1CFEAAD6-F5F2-4B2A-BA85-A16A9E8EB35C | python -mjson.tool```

The output of this command which searches by uuid looks something like this:

```
{
    "genomes": [
        {
            "created_by": "<public>",
            "created_on": "1339626977481",
            "deleted": true,
            "id": "24",
            "last_modified_by": "<public>",
            "last_modified_on": "1339626977481",
            "name": "Selaginella moellendorffii ENA1 (Ensembl 13)",
            "path": "/data2/collections/genomeservices/0.2/Selaginella_moellendorffii.ENA1/de_support/",
            "uuid": "1CFEAAD6-F5F2-4B2A-BA85-A16A9E8EB35C"
        }
    ],
    "success": true
}
```

#### Deleting a Genome Reference

Endpoint: DELETE **/genome-reference/{uuid}**.

Deleting a genome reference will mark the deleted field of the genome reference as true. It is important to understand that the genome_reference is still in the database and is not deleted, only the boolean field `deleted` is flagged from true to false.

#####Example
If you want to DELETE the genome reference identified as: 0CF5BA6C-9DE6-4D3D-A9B9-66FC19F6G4E8, the command looks like this:

```curl -vXDELETE http://localhost:3000/secured/genome-reference/0CF5BA6C-9DE6-4D3D-A9B9-66FC19F6G4E8```

Now the genome reference specified by the uuid will be marked as deleted.

An incorrectly formed uuid will return a success body but with an empty array of genomes.
If it looks like this you did it wrong:

```json
{
    "genomes": [],
    "success": true
}
```

On successful 'deletion' the response will look something like this:

```
{
    "genomes": [
        {
            "created_by": "<public>",
            "created_on": "1339626977481",
            "deleted": true,
            "id": "8",
            "last_modified_by": "ipctest@iplantcollaborative.org",
            "last_modified_on": "1339629596968",
            "name": "The JOHN species",
            "path": "/bacon/dennis",
            "uuid": "B7FE13D8-A9E0-408C-946F-5E3AFB922E7D"
        }
    ],
    "success": true
}
```
There is an alternate way to delete the genome reference through the modify genome reference endpoint (POST /genome-reference/). Using the modify endpoint you can un-delete previously deleted endpoints as well.

#### Modifying a Genome Reference

Endpoint: POST **/genome-reference**.

To modify a current genome reference you need to form a JSON body and pass it in with the request.

1. The new **name** of the genome. **required**
2. The new **path** to the directory of where the genome is located. **required**
3. The **deleted** field marked true/false. **optional**
4. The **uuid** referencing the reference. **This will not be modified**

```json
{
    "name": new-genome-name,
    "path": new-path-to-genome,
    "deleted": true-false-optional,
    "uuid": uuid-referencing existing genome
}
```

If you want to only change one of the parameters, you will still need to pass the required parameters 'name', and 'path'. Just pass the name that was already in the database.

Here is an example command:

```
    curl -vXPOST -d
    '{
        "name": "New Species Name",
        "path": "home/research/new",
        "uuid": "970E8ED8-B65D-46FA-849E-03DB344367DC"
    }'
    http://localhost:3000/secured/genome-reference
```

When modifying a genome reference, the user and when it was modified will be updated.

You can use the modify endpoint to delete/un-delete genome references.

Here is an example command which will also 'delete' the reference:

```
    curl -vXPOST -d
    '{
        "name": "New Species Name",
        "path": "home/research/new",
        "deleted": true,
        "uuid": "970E8ED8-B65D-46FA-849E-03DB344367DC"
    }'
    http://localhost:3000/secured/genome-reference
```

Potential 500 errors: *Passing the uuid without the path or name fields in the JSON body.


#### Creating a New Genome Reference

Endpoint: PUT **/genome-reference**.

You will need to form a JSON body with two parameters and pass it in with the request.

1. The **name** of the genome. **required**
2. The **path** to the directory where the genome is located. **required**

The uuid and the user info is all automatically generated.

```json
{
    "name": genome-name,
    "path": path-to-genome
}
```
Here is an example command:

```
    curl -vXPUT -d
    '{
        "name": "Alien Species",
        "path": "/home/users/genomes"
    }'
    http://localhost:3000/secured/genome-reference
```

**NOTE:** If for some reason the CAS ticket username is not already in the database, it will be added as a user and the id of that user will be set as the creator of the genome reference.

---

### Unrecognized Service Paths

If an unrecognized service path is used in a request to Conrad then the response
will contain a success flag along with a message indicating that the service
path is unrecognized:

```
dennis$ curl -s http://by-tor:14444/secured/foo | python -mjson.tool
{
    "reason": "unrecognized service path",
    "success": false
}
```

Potential 500 errors: *Not forming a JSON body with both the path and name.
