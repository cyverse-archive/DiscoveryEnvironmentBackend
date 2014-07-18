# irods-avu-migrator

A command-line tool for migrating AVU metadata from iRODS and into the DE's Metadata PostgreSQL database.
This tool assumes that there are not very many data objects with metadata template AVUs, so it pre-loads all AVUs into memory with the attribute "ipc-metadata-template", then converts the AVUs for each data object with this AVU one at a time.

