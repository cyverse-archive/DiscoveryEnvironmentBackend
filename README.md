# facepalm

Database migration and management tool.

## Usage

```
# Reinitialize the database using the database tarball from the latest QA drop.
facepalm -h hostname -U username -d database -q latest -f filename.tar.gz

# Explicitly fetch the database tarball from the QA drop on 6/15/2012.
facepalm -h hostname -U username -d database -q 2012-06-15 -f filename.tar.gz

# Reinitiaize the database using a local database tarball.
facepalm -h hostname -U username -d database -f /path/to/filename.tar.gz

# Obtain the database tarball from the latest build of a Jenkins job.
facepalm -h hostname -U username -d database -j jobname -f filename.tar.gz

# Upgrade the database from the existing version using the conversions in the
# database tarball from the latest QA drop.
facepalm -m update -h hostname -U username -d database -q latest -f filename.tar.gz

# Upgrade the database from the existing version using the conversions in the
# database tarball from the QA drop on 6/15/2012.
facepalm -m update -h hostname -U username -d database -q 2012-06-15 -f filename.tar.gz

# Upgrade the database from the existing version using the conversions in a
# local database tarball.
facepalm -m update -h hostname -U username -d database -f /path/to/filename.tar.gz

# Upgrade the database from the existing version using the conversions in the
# database tarball from the latest build of a Jenkins job.
facepalm -m update -h hostname -U username -d database -j jobname -f filename.tar.gz

# Obtaining help.
facepalm -?
```

## Required Arguments

All of the arguments that are required have default values, so these arguments
may not have to be explicitly defined on the command line unless the default
settings won't work in your case.

### -m --mode

Default value: `init`

This argument indicates whether facepalm should update or completely initialize
the database. If the mode is set to `update` then the database schema will be
updated without clobbering any data in the database (unless some database rows
are _supposed_ to be clobbered as part of a database conversion). If this mode
is set to `init` then the database will be completely reinitialized.

### -h --host

Default Value: `localhost`

This argument indicates the name of host where the database resides. If this
argument is not specified, facepalm will attempt to connect to the database
server on the local host.

### -p --port

Default Value: `5432`

This argument indicates which port number to use when attempting to connect to
the datase. If this argument is not specified, facepalm will attempt to connect
to port `5432` (the default listen port for PostgreSQL).

### -d --database

Default Value: `de`

This argument indicates the name of the database to update. If this argument is
not specified, faceplam will update the `de` database.

### -U --user

Default Value: `de`

This argument indicates the username to use when authenticating to the
database. If this argument is not specified, facepalm will use `de` for the
username. Note that the database user must own both the database and the public
schema in the database.

There is no command-line option to specify the password. Instead, facepalm will
attempt to look up the password in the `.pgpass` file. If the password can't be
obtained from `.pgpass`, facepalm will prompt the user for the password. Because
facepalm uses JDBC to connect to the database, only one connection to the
database is established per facepalm invocation. Because of this, there will be
at most one password prompt per facepalm invocation.

## Options

### -j --job

This argument indicates the name of a Jenkins job from which the database
initialization scripts can be obtained. At the time of this writing, valid job
names are `database` and `database-dev`. This option is used in conjunction with
the `-f` or `--filename` option to specify the name of the Jenkins job along
with the name of the build artifact produced by the job. If the file name is not
specified explicitly then facepalm assumes that it is `database.tar.gz`.

This argument is one of three command-line arguments that can be used to
indicate where facepalm should obtain its database initialization files. The
other two options are `-q`, which is equivalent to `--qa-drop`, and `-f`, which
is equivalent to `--filename`.

### -q --qa-drop

This argument indicates the QA drop from which the database initialization
scripts should be obtained. This can either be the date of the QA drop in
`YYYY-MM-DD` format (for example, `2012-06-12`) or the literal string `latest`
to indicate that facepalm should obtain the database initialization scripts from
the most recent QA drop. This option is used in conjunction with the `-f` or
`--filename` option to specify the location of the QA drop along with the name
of the file containing the database initialization scripts. If the file name is
not specified explicitly then facepalm assumes that it is `database.tar.gz`.

This argument is one of three command-line arguments that can be used to
indicate where facepalm should obtain its database initialization files. The
other two options are `-j`, which is equivalent to `--job`, and `-f`, which is
equivalent to `--filename`.

### -f --filename

Default Value: `database.tar.gz`

This argument is used to specify the name of the tarball containing the database
initialization scripts. If this argument is used by itself (that is, without the
`-j` or `-q` options) then facepalm will treat its value as the path to a
tarball on the local file system. If this argument is used in conjunction with
the `-j` or `-q` options then facepalm will treat its value as the name of the
tarball containing the database initialization scripts on the remote server.

### -? --help --no-help

The `-?` and `--help` options can be used to tell facepalm to display a help
message. The `--no-help` message can be used to indicate that help should not be
displayed, but this is the default behavior anyway.

### --debug --no-debug

The `--debug` option can be used to tell facepalm to display additional
debugging information, which can be helpful in troubleshooting database
initialization problems. The `--no-debug` option can be used to disable
debugging, but this is the default behavior anyway.

## Diagnostics

### ::required-options-missing

No value was provided for a required option. This error is unlikely to occur in
practice because all of the required arguments have default values. If this does
occur, verify that the command line is correct.

### ::build-artifact-retrieval-failed

The utility attempted to get the database build artifact from a remote URL and a
status that was less than 200 or greater than 299 was returned. Verify that the
specified QA drop or Jenkins job name is correct.

### ::database-tarball-copy-failed

The utility was unable to copy the database tarball to the temporary directory.
Verify that the file system is not full or write protected.

### ::command-execution-failed

The utility was unable to execute a subcommand. The subcommand should be listed
in the error message. Verify that the required subcommand is installed on the
system and that its location is in the `PATH` environment variable.

### ::build-artifact-expansion-failed

The utility attempted to extract the database initialization files from the
database build artifact and `tar` returned a non-zero exit status. Verify that
the build artifact is a valid tar file.

### ::temp-directory-creation-failure

The utility attempted and was unable to create a temporary directory within the
current working directory. Verify that you have write permission to the current
working directory.

### ::database-connection-failure

The utility was unable to connect to the database. Verify that the database
connection settings are correct and, if applicable, that the `.pgpass` file is
formatted correctly.

### ::unknown-mode

The specified mode is not recognized by the utility. Verify that the command
line is correct.

### ::conversion-validation-error

Some database conversion scripts contain sanity checks; this is the error that
is raised if one of the sanity checks fails. Gather the information requested in
the error message and forward it to the Core Software team.

### ::no-password-supplied

The database authentication password could not be found in the '.pgpass' file
and no terminal is attached to the utility (so the utility can't prompt for the
password). Verify that the '.pgpass' file is formatted correctly and has an
entry for the desired database. If you want the utility to prompt you for the
password, be sure to run the utility in the foreground.

### ::incompatible-database-conversion

One of the database conversions in the database tarball has a version number
that is greater than the maximum compatible database version defined in
kameleon. Verify that the correct database tarball and facepalm version are
being used. If everything appears to be correct, contact Core Software to ensure
that kameleon has been updated correctly.

### Other Errors

Other errors represent uncaught exceptions. The most likely cause of these
errors is a failure to connect to or update the database. Verify that the
command line is correct, paying close attention to the database connection
settings. It might also be helpful to review the exception itself to find out if
it contains any useful information. If you're unable to find the cause of the
problem, please send a copy of the command line, the exception and the full
stack trace, if there is one, to the Core Software group.

## Bugs and Limitations

There are no known bugs in facepalm at this time. Please report problems to
the Core Software group. Patches are welcome.

## License and Copyright

Copyright (c) 2012, The Arizona Board of Regents on behalf of The University
of Arizona

All rights reserved.

Developed by: iPlant Collaborative at BIO5 at The University of Arizona
http://www.iplantcollaborative.org

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

 * Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

 * Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

 * Neither the name of the iPlant Collaborative, BIO5, The University of
   Arizona nor the names of its contributors may be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
