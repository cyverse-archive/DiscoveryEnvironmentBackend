# Table of Contents

* [Reference Genome endpoints](#reference-genome-endpoints)
    * [Exporting Reference Genomes](#exporting-reference-genomes)
    * [Importing Reference Genomes](#importing-reference-genomes)

# Reference Genome endpoints

Note that secured endpoints in data-info and metadactyl are a little different from
each other. Please see [data-info Vs. Metadactyl](data-info-v-metadactyl.md) for more
information.

## Exporting Reference Genomes

Secured Endpoint: GET /secured/reference-genomes

Delegates to metadactyl: GET /secured/reference-genomes

This endpoint is a passthrough to the metadactyl endpoint with the same
path. Please see the metadactyl documentation for more details.

## Importing Reference Genomes

Secured Endpoint: PUT /secured/reference-genomes

Delegates to metadactyl: PUT /secured/reference-genomes

This endpoint is a passthrough to the metadactyl endpoint with the same
path. Please see the metadactyl documentation for more details.
