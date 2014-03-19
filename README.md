porklock
========

This tool is responsible for transferring files from an analysis directory into iRODS. It is usually called as part of a Condor DAG.

## CONFIGURATION

Make sure that the .irodsEnv and .irodsA files are located in ~/.irods/ for the system-level user that will be running the filetool.

Make sure that the imkdir, ils, and iput commands are available in a directory that is on the system's PATH. 

filetool uses the .irodsEnv and .irodsA files to configure itself and the i-commands that it runs. It will search the home directory of the user running the tool for the .irods/ directory and will use the .irodsEnv and .irodsA file contained within. Additionally, it will search the user's PATH for the imkdir, ils, and iput commands. It will use the first matches that it encounters.

No additional configuration files are needed.


## COMMAND LINE OPTIONS

Note: If you don't use the '--exclude' or '--include' options, then every file in '--source' will be transferred into '--destination'. The operation is recursive.

#### --destination    

The path to the directory on the iRODS server that the files should be placed in. This flag is required.


#### --exclude    

A list of files to exclude from the transfer. By default, the list is delimited by commas, but you can set your own delimiter with the '-exclude-delimiter' option. Note, if you specify a file that doesn't exist you will NOT get an error message. This is intentional. It allows scripts to define a default set of excluded files without worrying about errors if the excluded file does not exist. This flag is optional.


#### --exclude-delimiter    

The delimiter used in the '-exclude' option. It defaults to ','. This flag is optional.


#### --include    

A whitelist of files to transfer. If this option is used, then any files that aren't included in this list will NOT be transferred into iRODS. Also, if a file is included in both the '-include' and '-exclude' lists, then it will NOT be transferred into iRODS. In other words, the '-exclude' option overrides the '-include' option. This flag is optional.


#### --include-delimiter    

The delimiter used in the '-include' option. It defaults to ','. This flag is optional.


#### --single-threaded    

This option will cause the 'iput' commands that are executed by filetool to run in single-threaded mode. In other words, it adds the '-N 0' flag to the options passed to the 'iput' command. This does NOT alter change the threadedness of filetool itself. This flag is optional.


#### --source    

The path to the directory on the local system that contains files to be transferred into iRODS. Only one directory can be specified at a time. This flag is required.

