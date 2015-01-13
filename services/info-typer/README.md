# info-typer

An AMQP message based info type detector

info-typer listens to the `irods` AMQP topic exchange for iRODS change messages related to data
objects. The iRODS change messages are described in the document
https://docs.google.com/document/d/126uSOX8VWfFyRub1Ibqiknf4QbQuDZLOCktoBLpgqsE/edit. For each
`data-object` change message, info-typer attempts to detect the info type of the corresponding file.
If it succeeds, it updates the `ipc-filetype` AVU in iRODS with the detected info type.

This is not a public facing service. It has no API.