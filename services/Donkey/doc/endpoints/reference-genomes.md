# Table of Contents

* [Reference Genome endpoints](#reference-genome-endpoints)
    * [Exporting Reference Genomes](#exporting-reference-genomes)
    * [Get a Reference Genome by ID](#get-a-reference-genome-by-id)
    * [Importing Reference Genomes](#importing-reference-genomes)
    * [Deleting Reference Genomes](#deleting-reference-genomes)
    * [Updating Reference Genomes](#updating-reference-genomes)

# Reference Genome endpoints

Note that secured endpoints in Donkey and metadactyl are a little different from
each other. Please see [Donkey Vs. Metadactyl](donkey-v-metadactyl.md) for more
information.

## Exporting Reference Genomes

Secured Endpoint: GET /reference-genomes

Delegates to metadactyl: GET /reference-genomes

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.

## Get a Reference Genome by ID

Secured Endpoint: GET /reference-genomes/{reference-genome-id}

Delegates to metadactyl: GET /reference-genomes/{reference-genome-id}

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.

## Importing Reference Genomes

Secured Endpoint: PUT /admin/reference-genomes

Delegates to metadactyl: PUT /admin/reference-genomes

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.

## Deleting Reference Genomes

Secured Endpoint: DELETE /admin/reference-genomes/{reference-genome-id}

Delegates to metadactyl: DELETE /admin/reference-genomes/{reference-genome-id}

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.

## Updating Reference Genomes

Secured Endpoint: PATCH /admin/reference-genomes/{reference-genome-id}

Delegates to metadactyl: PATCH /admin/reference-genomes/{reference-genome-id}

This endpoint is a passthrough to the metadactyl endpoint with the same path.
Please see the metadactyl documentation for more details.
