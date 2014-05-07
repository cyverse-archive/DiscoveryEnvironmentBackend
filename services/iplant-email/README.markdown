iplant-email
============

Provides backend services with a relatively simple way to send emails.


Installation
------------

iplant-email is packaged as an RPM and can be installed using the command,
`yum install iplant-email`.

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
