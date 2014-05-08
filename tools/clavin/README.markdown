Description
-----------

Clavin is a basic command-line tool for dealing with configuration files at
iPlant.  Its primary purposes are to:

* generate configuration files from templates.


Overview
--------

On a machine where WAR files are being deployed:

```
clavin files -f myenvironments.clj -t /path/to/template-dir -a app -e env -d
deployment --dest /path/to/dest-dir
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
clavin edn -h
```

Creating Configuration File Templates
-------------------------------------

Clavin uses configuration file templates in conjunction with
deployment-specific configuration setting definitions in order to generate
configuration settings.

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

Generating Properties Files
---------------------------

Clavin provides a way to generate Java properties files from templates and 
environments files.  The subcommand used to generate properties files is `files`.  

```
clavin files --dest <output-dir> -f <path-to-environments-file> -t <path-to-template-dir> -a app -e dev -d dep-2 service-name1 service-name-2
```

The only command-line option that is unique to the `files` subcommand is
--dest, which is used to specify the path to the output directory.  All of the
generated properties files will be placed in this directory.  The file names
will be the service name with a `.properties` extension.  For example, if you
generate a properties file for the service, `discoveryenvironment` then the
name of the generated file will be `discoveryenvironment.properties`.


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
