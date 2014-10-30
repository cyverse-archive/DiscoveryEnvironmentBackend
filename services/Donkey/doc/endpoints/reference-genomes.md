# Table of Contents

* [Reference Genome endpoints](#reference-genome-endpoints)
    * [Exporting Reference Genomes](#exporting-reference-genomes)
    * [Importing Reference Genomes](#importing-reference-genomes)

# Reference Genome endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Exporting Reference Genomes

Secured Endpoint: GET /reference-genomes

Delegates to metadactyl: GET /reference-genomes

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.

## Importing Reference Genomes

Secured Endpoint: PUT /admin/reference-genomes

Delegates to metadactyl: PUT /admin/reference-genomes

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.
