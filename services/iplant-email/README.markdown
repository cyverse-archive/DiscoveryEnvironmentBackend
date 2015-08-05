iplant-email
============

Provides backend services with a relatively simple way to send emails.


Installation
------------

iplant-email is packaged as an RPM and can be installed using the command,
`yum install iplant-email`.

Alternatively, iplant-email can be run using [Docker](https://www.docker.com);
more details are in the "Docker" section below.

Configuration
-------------

iplant-email gets its configuration settings from a configuration file. The path
to the configuration file is given with the --config command-line setting.

Here's an example configuration file in properties file format:

```properties
iplant-email.smtp.host=smtp.example.org
iplant-email.smtp.from-address=noreply@example.org
iplant-email.app.listen-port=3000
```

Making a request
----------------

There is only one endpoint in the iplant-email app, "/". To request that an
email be sent, you need to know the template name, the 'to' email address, and
the values that are to be interpolated into the email template.

Here is a sample request:

```
curl -H "Content-Type:application/json" -d '
{
    "to" : "foo@example.com",
    "from-addr" : "support@example.com",
    "from-name" : "Example Support",
    "subject" : "Example",
    "template" : "bettertest",
    "values" : {
        "user" : "Foo",
        "useremail" : "foo@example.com",
        "value" : "foobar"
    }
}
' http://127.0.0.1:3000/
```

The 'to' field is hopefully pretty self-explanatory, as is the 'subject'
field.

The from-addr field is optional and contains the email address that should be placed in the from field. If it is omitted from the request, the configuration option from iplant-emails config file will be used instead.

The from-name field is optional and contains the name associated with the email address placed in the from field. If it is omitted, then no name is associated with the from adress.

The 'template' field corresponds to an email template located in the
`/etc/iplant-email` directory. It is the filename of the template, minus the
`.st` extension.

The values field is a map that contains values that are interpolated into
corresponding fields in the email template. For instance, "Foo" is substituted
into the email wherever $user$ appears.


Adding/Modifying Email Templates
--------------------------------

Email templates live in `/etc/iplant-email`. 

You can add and modify templates on the fly, iplant-email will pick up the
changes without having to be restarted.

All template files must end with a '.st' extension. The name of the template
is derived from the part of the filename that comes before the '.st'
extension.

The syntax for templatizing emails is available in the "Learn basic
StringTemplate syntax" section on this website:
http://www.antlr.org/wiki/display/ST/Five+minute+Introduction

Docker
------

In order to run iplant-email within Docker, you'll need to first create a standalone jar file and then a docker container. To do the former, follow the instructions at the top level of this repository using build_all.clj, which should produce `iplant-email.standalone.jar` in a `target` directory. To build a docker container, run `docker build -t iplant-email .` from the root iplant-email directory (the one with the Dockerfile in it).

Once a docker image has been built, it can be run with the `docker run` command, but ensure that a configuration file is available to the process as well using the `-v` option; to access iplant-email from the host system, forward ports using `-P` or `-p`. For example, my configuration has:

```properties
iplant-email.smtp.host=smtp.example.org
iplant-email.smtp.from-address=noreply@example.org
iplant-email.app.listen-port=60000
```

I've saved this file to `~/conf-files/iplant-email.properties`. Then, to run iplant-email: `docker run --rm --name iplant_email -p 3000:60000 -v ~/conf-files/iplant-email.properties:/home/iplant/conf.properties iplant-email --config conf.properties`. (the `--name` configures the container name, and `--rm` removes the container after it's shut down; the double appearance of `conf.properties` is because the first (with `-v`) mounts the file in the container, where the second passes the path to iplant-email). If you'd prefer, add `-d` to daemonize the docker container.

Once it's running, iplant-email can be accessed from port 3000 (or whatever port you configured externally using `-p`, or whatever port was chosen randomly by `-P`) on the IP of the machine running Docker. If you're using Docker locally, this should be http://127.0.0.1:3000/; if you're using Boot2Docker it will be on the VM's IP address, which means that from a shell (e.g. with curl) `http://$(boot2docker ip):3000/` should reach iplant-email.
