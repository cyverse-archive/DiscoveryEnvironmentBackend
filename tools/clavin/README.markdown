Description
-----------

Clavin is a basic command-line tool for dealing with configuration files at
iPlant.  Its primary purposes are to:

* load ACLs into Zookeeper;

* load configuration settings into Zookeeper;

* generate configuration files from templates.


Overview
--------

On an admin machine (admin machines are described below):

```
clavin hosts --host 127.0.0.1 --acl myacls.properties
clavin props --host 127.0.0.1 --acl myacls.properties -f myenvironments.clj -t
/path/to/template-dir -a app -e env -d deployment
```

On a machine where WAR files are being deployed:

```
clavin files -f myenvironments.clj -t /path/to/template-dir -a app -e env -d
deployment --dest /path/to/dest-dir
```

To list current configuration settings on a services host:

```
clavin get-props --host 127.0.0.1 -s service-name prop-name-1 prop-name-2
```

To list current configuration settings on another host:

```
clavin get-props --host 127.0.0.1 --service-host somehost -s service-name prop-name-1 prop-name-2
```

Environment listing and validation:

```
clavin envs -l -f myenvironments.clj
clavin envs -v -f myenvironments.clj
```

Getting help:

```
clavin help
clavin envs -h
clavin files -h
clavin hosts -h
clavin props -h
```

Specifying Zookeeper Connection Settings
----------------------------------------

Zookeeper connection settings can be specified in one of two ways: either by
specifying the host and port directly on the command line or by reading the file
that the services use to obtain the connection settings, `zkhosts.properties`.
In the former case, the options to use are `--host` and `--port`.  The `--port`
setting defaults to `31381` if it's not specified.  The `--host` option defaults
to nil, meaning that Clavin will look for `zkhosts.properties`.  For a concrete
example, suppose you wanted to list the properties for the service, `foo`, on
the Zookeeper process running on the local host and listening on the standard
port.  The following command could be used for that:

```
$ clavin get-props --host localhost -s foo
```

Now suppose that Zookeeper is listening on port 2001 instead.  In that case, the
equivalent command would look something like this:

```
$ clavin get-props --host localhost --port 2001 -s foo
```

If you have a copy of `zkhosts.properties` in a specific location on the local
machine then you can tell Clavin to obtain the connection settings from that
file using the `-z` or `--zkhosts-path` command-line option.  If you wanted to
use the copy of `zkhosts.properties` in your home directory, for example, then
the equivalent command from the example above would be something like this:

```
$ clavin get-props -z ~/zkhosts.properties -s foo
```

Finally, if your copy of zkhosts.properties is located in the default location
(`/etc/iplant-services/zkhosts.properties`), then you can leave the connection
settings off of the command line completely:

```
$ clavin get-props -s foo
```

Note that these options apply to all sub-commands that interact with Zookeeper,
but the examples below will all specify `--host` directly.

Creating an ACLs file
---------------------

An "ACL file" is simply a java properties file that lists the ip-addresses of
machines that are in a particular environment (dev, qa, stage, prod,
etc.). Each environment can have multiple deployments. For instance, dev has
dep-1 and dep-2. Here's the ACLs file for the dep-1 deployment (you can list
multiple deployments in one file).

```
app.dev.dep-1 = 192.168.1.1,\
                192.168.1.2,\
                192.168.1.3,\
                192.168.1.4,\
                192.168.1.5

admin = 192.168.1.5,\
        192.168.1.6,\
        192.168.1.7
```

The naming of the keys is significant. Any machine listed in the "admin" value
will be given admin privileges in Zookeeper for the deployments included in
the ACLs file.

The "app.dev.dep-1" key tells clavin the application name, environment, and
deployment that is being described. In this case, "app" is the application
name, "dev" is the environment, and "dep-1" is the deployment. These values
correspond with the \--app (-a), \--env (-e), and \--deployment (-d) options
that clavin uses when setting properties in Zookeeper. More on that below.

The values associated with each key in the ACLs file are comma separated lists
of IP addresses.


High level guidelines for organizing ACL files
----------------------------------------------

* Don't reuse the same IP address in multiple deployments.

* If an IP address appears in a deployment and as an admin machine, then that
  deployment and admin section should be in the same ACL file.

* If an IP address appears in multiple admin sections in different files, then
  consider merging those files.


Loading ACLs file into Zookeeper
--------------------------------

Once you've got the ACLs file put together, you load it into Zookeeper with
the following command:

```
clavin hosts --host 127.0.0.1 --acl <path-to-acls-file>
```

That will create entries in /hosts on Zookeeper for each machine and tell what
deployments it's associated with (deployments are analogous to groups in this
case).

For instance, since 192.168.1.5 is listed both as an admin machine and as a
machine in the "app.dev.dep-2" deployment, Zookeeper will create the following
nodes for it:

```
[zk: localhost:31381(CONNECTED) 2] ls /hosts/192.168.1.5

[app.dev.dep-2, admin]
```

In other words, the /hosts/192.168.1.5/app.dev.dep-2 and
/hosts/192.168.1.5/admin nodes both exist. The ACLs on the "admin" node are as
follows:


```
[zk: localhost:31381(CONNECTED) 4] getAcl /hosts/192.168.1.5/admin

'ip,'192.168.1.5

: cdrwa

'ip,'192.168.1.6

: cdrwa

'ip,'192.168.1.7

: cdrwa

'ip,'192.168.1.5

: rw
```

Translation: Every admin node is listed as having admin access (the "cdrwa"
part) and the node itself has read-write access. This means that other
machines can't accidentally or maliciously nuke the group/deployment
memberships unless they are listed as an admin node in the ACLs file.


Creating Configuration File Templates
-------------------------------------

Clavin uses configuration file templates in conjunction with
deployment-specific configuration setting definitions in order to generate
configuration settings.  Once the configuration settings have been generated,
they may either be loaded directly into Zookeeper or written to a file for
inspection or use by a web application.

The configuration file templates used by Clavin consist of plain text with
configuration placeholders interspersed throughout the text.  The placeholders
consist of property names surrounded by dollar signs.  For example:

```
foo.bar.baz = $some_property$
```

The property names should be valid java identifiers.  (That is, they should
begin with either an underscore or an alpha character and be followed by zero
or more underscores or alphanumeric characters.)  The preferred naming
convention is to separate words in identifiers with underscores rather than to
use camelCase.  The reason for this has to do with the definition of the
placeholder values, which will be described in the _Creating an Environments
File_ section.

Literal dollar signs can be included the file by preceding the dollar sign
with a backslash:

```
dr.evil.catch-phrase = ...unless you bring me \$1,000,000!
```

Each service has its own configuration file template, with the template file
named after the service with a extension of `.st`.  For example, the service,
`donkey`, would have a template file named `donkey.st`.  (The `.st` file name
extension stands for "String Template", which is the library used to process
the template files.)  Here's an example configuration file template,
`nibblonian.st`:

```
# Jargon-related configuration
nibblonian.irods.host            = $irods_host$
nibblonian.irods.port            = $irods_port$
nibblonian.irods.user            = $irods_user$
nibblonian.irods.password        = $irods_password$
nibblonian.irods.home            = $irods_home$/
nibblonian.irods.zone            = $irods_zone$
nibblonian.irods.defaultResource = $irods_default_resource$

# Application-specific configuration

# Controls the size of the preview. In bytes.
nibblonian.app.preview-size = 8000

# The size (in bytes) at which a preview becomes a rawcontent link in the
# manifest.
nibblonian.app.data-threshold = 8000

# The maximum number of attempts that nibblonian will make to connect if the
# connection gets severed.
nibblonian.app.max-retries = 10

# The amount of time (in milliseconds) that the nibblonian will wait between
# connection attempts.
nibblonian.app.retry-sleep = 1000

# Set this value to 'true' to send deleted files and folders to the Trash.
nibblonian.app.use-trash = true

# A comma delimited list of file/directory names that should be filtered from
# Nibblonian listings.
nibblonian.app.filter-files = cacheServiceTempDir

# The path to the community data directory in iRODS
nibblonian.app.community-data = $irods_home$/shared

#The port nibblonian should listen on
nibblonian.app.listen-port  = $nibblonian_port$

#REPL-related configuration
nibblonian.swank.enabled =
nibblonian.swank.port =
```

If the name of the template has a file extension prior to the `.st` , eg. `name.ext.st`, the name
of the generated configuration file will be just the template name, eg. `name.ext`. Otherwise, the
name of the generated file will be the template name with `.properties` extension added. 

Listing and Validating Configuration File Templates
---------------------------------------------------

When generating configuration settings, it can be useful to list the available
templates.  Also, when a template file is modified or created, it can be
useful to verify that it is formatted correctly before attempting to generate
configuration settings from it.  Clavin provides the `templates` subcommand
for these settings:

```
clavin templates -t <path-to-template-dir> [-l|-v]
```

Here's an example template file listing:

```
$ clavin templates -l -t ~/tmp/templates/
belphegor-confluence
belphegor
buggalo
confluence
conrad
discoveryenvironment
donkey
iplant-email
jex
metadactyl
nibblonian
notificationagent
osm
panopticon
scruffian
```

Here's an example successful template file validation:

```
$ clavin templates -v -t ~/tmp/templates/
All templates are valid.
```

Here's an example unsuccessful template file validation:

```
$ clavin templates -v -t ~/tmp/templates/
belphegor-confluence is invalid: #<STLexerMessage 4:77: invalid character '-'>
belphegor-confluence is invalid: #<STCompiletimeMessage 4:78: 'base' came as a complete surprise to me>
Errors were found.
```

Validating Templates Against an Environments File
-------------------------------------------------

Clavin also provides the ability to validate templates against an environments
file to verify that there are no undefined properties referenced in the
templates and no unused properties defined in the environments file.  To do
this, simply validate the templates and include the path to the environments
file.

Here's an example unsuccessful validation:

```
$ clavin templates -t ~/tmp/templates -f environments.clj -v
Unused parameters were detected in dev.de-1:
	 :db-max-idle-minutes
Unused parameters were detected in dev.de-2:
	 :db-max-idle-minutes
Unused parameters were detected in qa.penguin:
	 :db-max-idle-minutes
Unused parameters were detected in qa.qa-1:
	 :db-max-idle-minutes
Unused parameters were detected in qa.qadb:
	 :db-max-idle-minutes
Unused parameters were detected in prod.prod:
	 :db-max-idle-minutes
Unused parameters were detected in staging.staging:
	 :db-max-idle-minutes
Errors were found.
```

Creating an Environments File
-----------------------------

The environments file defines the deployment-specific settings that are
plugged into the templates in order to generate the full-blown configuration
settings.  This file is just a plain Clojure source file containing a single
nested map definition:

```clojure
{:first-setting
 {[:env-1 :dep-1] "foo"
  [:env-1 :dep-2] "bar"
  [:env-2 :dep-3] "baz"}
 :second-setting
 {[:env-1 :dep-1] "quux"
  [:env-1 :dep-2] "ni"
  [:env-2 :dep-3] "ecky"}}
```

Note that the keys of the nested maps are vectors containing keywords which
correspond to the environment (from the `-e` or `--env` command-line option) and
the deployment (from the `-d` or `--deployment` command-line option),
respectively.  The app (from the `-a` or `--app` command-line option) is not
currently used in this file.

The keywords at the first level of the map correspond directly to the
placeholder names.  When clavin defines the values that will be used by the
template, it uses the keyword name with hyphens replaced with underscores as
the placeholder name.  For example, the `:first-setting` keyword mentioned
above will be used to define the value of the `first_setting` placeholder.
For example, suppose that you have the following template file:

```
some.service.first  = $first_setting$
some.service.second = $second_setting$
```

Also suppose that you've chosen the environment, `env-1` and the deployment
`dep-1` in the example environments file above.  If a property file is
generated from these settings, the resulting file would look like this:

```
some.service.first  = foo
some.service.second = quux
```

In some cases, it's convenient to be able to generate deployment-specific
settings from other deployment-specific settings.  For example, a service
generally has to be told which port to use for its listen port.  In addition
to this, clients of that service need to be given a base URL to use when
connecting to this service.  For this purpose, Clavin supports placeholders
inside of property values:

```clojure
{:foo-port
 {[:env-1 :dep-1] "8888"}
 :foo-base
 {[:env-1 :dep-1] "http://somehost.example.org:${foo-port}/bar"}}
```

Note that the placeholder format is a little different in this file than it is
in the configuration file templates.  The reason for this difference was to
make the substitution easier while still allowing idiomatic Clojure keywords
to be used in the settings map.

Clavin also supports chained substitutions, in which the substituted value
also contains a substitution string.  Take this (admittedly contrived)
environments file, for example:

```clojure
{:foo-port
 {[:env-1 :dep-1] "8888"}
 :foo-host
 {[:env-1 :dep-1] "somehost.example.org:${foo-port}"}
 :foo-base
 {[:env-1 :dep-1] "http://${foo-host}"}}
```

In this case, the value for the setting, `foo-base`, contains a reference to
the setting, `foo-host`, which contains a reference to the setting,
`foo-port`.  There's no practical limit to the number of settings in the
chain, but the chains should be kept as short as possible.  An extremely long
chain (or a recursive chain) will cause a stack overflow error in Clavin.
Checks for recursive chains can be added sometime in the future if this begins
to cause a problem.

Validating an Environments File
-------------------------------

In order for an environments file to be useful, every environment has to have
the same set of properties defined.  Clavin provides a way to ensure that this
is the case.  Here's an example of a valid file check:

```
$ clavin envs -v -f environments.clj
environments.clj is valid.
```

Here's an example of an invalid file check:

```
$ clavin envs -v -f environments.clj
environments.clj is not valid.

Please check the following properties for the following environments:
 :first-setting
 :second-setting
    [:dev :env-1]
    [:deb :env-1]
    [:dev :env-2]
    [:dev :enb-2]
```

In this case, it appears that some of the environment names were mistyped in one
or more properties.

Listing Environments in an Environments File
--------------------------------------------

The environments file can get fairly big, so it's sometimes convenient to be
able to list the environments that are defined within a file.  The envs
subcommand can be used for this purpose as well.

```
$ clavin envs -l -f environments.clj
environment deployment
----------- -----------
env-1       dep-1
env-1       dep-2
```

In this example, the environment definition file has one environment, `env-1`
containing two deployments, `dep-1` and `dep-2`.

Loading properties into Zookeeper
---------------------------------

You will need to have access to configuration file templates for the services
you're going to load the confgurations for and an environment definition file
that contains definitions for all of the placeholder values that are used in
the configuration files.  Then you can run the following command:

```
clavin props --host 127.0.0.1 --acl <path-to-acls-file> -f <path-to-environments-file> -t <path-to-template-dir> -a app -e dev -d dep-2 service-name1 service-name-2
```

The -a option refers to the app that you're loading configurations for.
Currently, the Discovery Environment is the only application supported by
Clavin, so the app will always be `de`.  Because of this, the default value
for the -a option is `de` and the -a option does not have to be specified.

The -e option refers to the environment that you're loading configurations
for, which corresponds to the name of the environment in the environments file
(or the environment and deployment listing described above).  In cases where
the deployment name is unique across all environments, the environment can be
determined from the deployment name, so it's not necessary to specify the -e
option unless another deployment of the same name appears in another
environment.

The -d option refers to the deployment that you're loading configurations
for.  Once again, this is the same as the name of the deployment in the
environments file (or the environment and deployment listing mentioned
above).  This option _must_ correspond to one of the deployments defined in
the environments file.

The values of the -a, -e, and -d options MUST correspond to a deployment
listed in the ACLs file indicated with the --acl option. For instance, in the
above example there must be a key of "app.dev.dep-2" with IP addresses
associated with it in the --acl file. You will get a stack trace if it doesn't
appear and nothing will get changed in Zookeeper.

The value of the -f flag should contain the path to the environment definition
file and the value of the -t flag should contain the path to the directory
containing the template files.  Note that all of the template files must have
an extension of `.st` and the name of the file without the extension
corresponds to the name of the service that is being configured.

The service name arguments correspond to the names of the services whose
configurations you want to load into Zookeeper.  For example, if you wanted to
load only the configurations for metadactyl and donkey, you would mention both
services on the command line:

```
clavin props --host 127.0.0.1 --acl acls.properties -f environments.clj -t templates -d dep-2 metadactyl donkey
```

To load all of the properties files without having to list them all, you can
simply not include any service names on the command line:

```
clavin props --host 127.0.0.1 --acl acls.properties -f environments.clj -t templates -d dep-2
```

Listing Configuration Settings in Zookeeper
-------------------------------------------

Once configuration settings have been loaded into Zookeeper, you can list them
using the `get-props` subcommand.  Suppose that you wanted to list all of the
configuration settings for the service, `foo` running on the local machine.  The
command to do that would look something like this:

```
clavin get-props --host 127.0.0.1 -s foo
```

The output from this command contains several lines containing the property name
followed by some whitespace an equals sign and the property value.  The output
from the previous command could look something like this:

```
foo.app.listen-port  = 65535
foo.bar.closing-time = 2:00
```

To improve the readability of the output, the equals signs are always aligned
with the property names and property values left-justified in their respective
columns.

If you're only interested in specific property values then you can specify the
property names on the command line using unnamed arguments.  For example, to
explicitly list the `foo.app.listen-port` and `foo.bar.closing-time` properties
from the example above, you could use something like this command:

```
clavin get-props --host 127.0.0.1 -s foo foo.app.listen-port foo.bar.closing-time
```

As a special case, the get-props subcommand will only display the property value
if only one property name is specified.  This is helpful for being able to
access property values from within shell scripts without having to parse the
output:

```
$ clavin get-props --host 127.0.0.1 -s foo foo.app.listen-port
65535
```

If you've read the previous sections, you may be wondering how Clavin determines
which deployment to look up in Zookeeper.  The answer is that it determines the
deployment the same way that the services themselves do: by looking up the IP
address of the service host in the ACLs stored in Zookeeper.  The service host
can be specified using the --service-host command-line option.  If the
--service-host option is not specified then the IP address of the local host
will be used.

As with all subcommands, the -h or --help option can be used to display a brief
usage message.  This usage message contains brief descriptions of all of the
command-line options.

Generating Properties Files
---------------------------

Not all iPlant services and web applications associated with the Discovery
Environment use Zookeeper to manage configuration settings.  For the
components that do not use Zookeeper, Clavin provides a way to generate Java
properties files from templates and environments files.  The subcommand used
to generate properties files is `files`.  The command line for the `files`
subcommand is similar to that of the `props` subcommand; the only differences
are that the ACL and Zookeeper connection settings aren't required and a
destination directory is required:

```
clavin files --dest <output-dir> -f <path-to-environments-file> -t <path-to-template-dir> -a app -e dev -d dep-2 service-name1 service-name-2
```

The only command-line option that is unique to the `files` subcommand is
--dest, which is used to specify the path to the output directory.  All of the
generated properties files will be placed in this directory.  The file names
will be the service name with a `.properties` extension.  For example, if you
generate a properties file for the service, `discoveryenvironment` then the
name of the generated file will be `discoveryenvironment.properties`.

See the `Loading properties into Zookeeper` section for details about the rest
of the command-line options.


Clavin command-line
-------------------

Running `clavin` with the `help` subcommand will list out the supported
sub-tasks:

```
$ clavin help
clavin envs|files|help|hosts|props|templates [options]
Each command has its own --help.
```

Running `clavin` without any arguments will provide a brief error message
along with the help text:

```
$ clavin
Something weird happened.
clavin envs|files|help|hosts|props|templates [options]
Each command has its own --help.
```

Currently, the supported subcommands are:

<table border='1'>
    <thead>
        <tr><th>Subcommand</th><th>Description</th></tr>
    </thead>
    <tbody>
        <tr><td>envs</td><td>Performs actions on environment files.</td></tr>
        <tr><td>files</td><td>Generates properties files.</td></tr>
        <tr><td>help</td><td>Displays a brief help message.</td></tr>
        <tr><td>hosts</td><td>Loads admin ACLs into Zookeeper.</td></tr>
        <tr><td>props</td><td>Loads configurations into Zookeeper.</td></tr>
        <tr><td>templates</td><td>Performs actions on templates.</td></tr>
    </tbody>
</table>

Here's the help message for the `envs` subcommand:

```
$ clavin envs --help
Usage:

 Switches                       Default  Desc
 --------                       -------  ----
 -h, --no-help, --help          false    Show help.
 -l, --no-list, --list          false    List environments.
 -v, --no-validate, --validate  false    Validate the environments file
 -f, --envs-file                         The file containing the environment definitions
```

Here's the help message for the `files` subcommand:

```
$ clavin files --help
Usage:

 Switches               Default  Desc
 --------               -------  ----
 -h, --no-help, --help  false    Show help.
 -f, --envs-file                 The file containing the environment definitions.
 -t, --template-dir              The directory containing the templates.
 -a, --app              de       The application the settings are for.
 -e, --env                       The environment that the options are for.
 -d, --deployment                The deployment that the properties files are for.
 --dest                          The destination directory for the files.
```

Here's the help message for the `hosts` subcommand:

```
$ clavin hosts --help
Usage:

 Switches               Default                                  Desc
 --------               -------                                  ----
 -h, --no-help, --help  false                                    Show help.
 --acl                                                           The file containing Zookeeper hostname ACLs.
 --host                                                          The Zookeeper host to connection to.
 --port                 31381                                    The Zookeeper client port to connection to.
 -z, --zkhosts-path     /etc/iplant-services/zkhosts.properties  The path to the file containing the Zookeeper connection settings.
```

Here's the help message for the `props` subcommand:

```
$ clavin props --help
Usage:

 Switches               Default                                  Desc
 --------               -------                                  ----
 -h, --no-help, --help  false                                    Show help.
 -f, --envs-file                                                 The file containing the environment definitions.
 -t, --template-dir                                              The directory containing the templates.
 --host                                                          The Zookeeper host to connect to.
 --port                 31381                                    The Zookeeper client port to connect to.
 -z, --zkhosts-path     /etc/iplant-services/zkhosts.properties  The path to the file containing the Zookeeper connection settings.
 --acl                                                           The file containing Zookeeper hostname ACLs.
 -a, --app              de                                       The application the settings are for.
 -e, --env                                                       The environment that the options should be entered into.
 -d, --deployment                                                The deployment inside the environment that is being configured.
```

Here's the help message for the `get-props` subcommand:

```
$ clavin get-props --help
Usage:

 Switches               Default                                  Desc
 --------               -------                                  ----
 -h, --no-help, --help  false                                    Show help.
 --host                                                          The Zookeeper host to connect to.
 --port                 31381                                    The Zookeeper port to connect to.
 -z, --zkhosts-path     /etc/iplant-services/zkhosts.properties  The path to the file containing the Zookeeper connection settings.
 -s, --service                                                   The service to get the settings for.
 --service-host                                                  The host that the service is running on.
```

Here's the help message for the `templates` subcommand:

```
$ clavin templates --help
Usage:

 Switches                       Default  Desc
 --------                       -------  ----
 -h, --no-help, --help          false    Show help.
 -l, --no-list, --list          false    List templates.
 -v, --no-validate, --validate  false    Validate templates.
 -t, --template-dir                      The directory containing the templates.
 -f, --envs-file                         The file containing the environment definitions.
```
