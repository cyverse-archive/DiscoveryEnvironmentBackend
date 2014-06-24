Chinstrap
=========

Chinstrap is iPlant's back-end 'front-end' ui that developers to visually see data about the DE-Database, and the OSM combined.

#Features So Far

* Day by Day job reports *_What was Running and When_*

`/info`

* Realtime view of Running and Submitted app flow.

`/apps`

* Unused Apps List *_Private apps are not included_*

`/components`

* Graph of all apps ran *_Data represents Completed apps only_*

`/graph`

* Integrator leaderboard, and detailed information about each integrator.

`/integrators`

* A page with links to the endpoints that return data.

`/raw`

## How do I build chinstrap locally?

That's a good question, well there are several things you will need to get
Chinstrap up and running locally.

1. First of all you need to make sure you check that you have configulon
   and it has the chinstrap.properties file. The file should have configuration
   settings to connect to both the OSM (Mongo) and the DE-Database (Postgres).

2. Use Clavin to load the chinstrap.properties file into zookeeper. The command
   that I've been using is:

   `clavin props --host 127.0.0.1 --acl ~/configulon/devs/de2-acls.properties --file ~/configulon/devs/de-2/chinstrap.properties -a de -e dev -d de-2`

3. If you haven't already clone chinstrap into your preferred directory.

4. Make sure you have leiningen with the noir plugin installed, with:

   `lein plugin install lein-noir 1.2.1`

5. then go into the top level of the chinstrap directory and type:

    `lein run`

   Wait for leiningen to download dependencies and compile Chinstrap.
   If it all worked it will display a jetty message with the [port #].

6. Go to localhost:[port #] and you should see Chinstrap up and running!

## TODO

1. The list of unused apps should have a link to the wiki page for
them or do a search on the forums if no wiki link has been provided.

2. Get detailed info about a certain day on graph bubble click.

## NOTE

Everything is subject to change.

## Created By

Roey Chasman
