Viewing a Genome File in CoGe
-----------------------------
A genome file may be submitted to CoGe for viewing within their genome viewer.
This endpoint will share the given genome files with the CoGe user, then submit those paths to their "genome load" service.
This service will return a URL where the authenticated user may view the genome viewer's progress, and the genome visualization once processing is done.

__URL Path__: /coge/genomes/load

__HTTP Method__: POST

__Error Codes__: ERR_NOT_A_USER, ERR_BAD_OR_MISSING_FIELD, ERR_DOES_NOT_EXIST, ERR_NOT_OWNER

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.

__Request Body__:

    {
        "paths": [
            "/iplant/home/ipctest/simple.fasta"
        ]
    }

__Response Body__:

    {
        "coge_genome_url": "http://bit.ly/CoGeSample"
    }

__Curl Command__:

    curl -sd '{"paths":["/iplant/home/ipctest/simple.fasta"]}' http://127.0.0.1:3000/secured/coge/load-genomes?proxyToken=$(cas-ticket)

Searching for Genomes in CoGe
-----------------------------
A user may search for genome information in CoGe in order to retrieve a text representation of
that genome for use in DE analyses.

__URL Path__: /coge/genomes

__HTTP Method__: GET

__Error Codes__: ERR_REQUEST_FAILED

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* search     - The string to search for.

__Response Body__:

    {
        "genomes": [
            {
                "chromosome_count": 35,
                "description": null,
                "id": 9716,
                "info": "Actinomyces coleocanis strain DSM 15436 (v1, id9716): unmasked",
                "link": null,
                "name": null,
                "organism": {
                    "description": "Bacteria; Actinobacteria; Actinobacteridae; Actinomycetales; Actinomycineae; Actinomycetaceae; Actinomyces",
                    "id": 23534,
                    "name": "Actinomyces coleocanis strain DSM 15436"
                },
                "organism_id": 23534,
                "restricted": false,
                "sequence_type": {
                    "description": "unmasked sequence data",
                    "id": "1",
                    "name": "unmasked"
                },
                "version": "1"
            },
            ...
        }
    }

Note: the response body from CoGe is passed back to the caller without modification.

__Curl Command__:

    $ curl -s "http://127.0.0.1:3000/coge/genomes?proxyToken=$(cas-ticket)&search=canis" | python -mjson.tool

Exporting CoGe Genome Data to iRODS
-----------------------------------
Once a user has found an interesting genome, he or she may request a text representation of the
genome to be stored in the iPlant Data Store for processing in the DE.

__URL Path__: /coge/genomes/{genome-id}/export-fasta

__HTTP Method__: POST

__Error Codes__: ERR_REQUEST_FAILED

__Request Query Parameters__:

* proxyToken - A valid CAS ticket.
* notify     - If present and set to "true" the user's email address will be forwarded to CoGe.
* overwrite  - If present and set to "true" the output file will be overwritten if it exists.

__Response Body__:

    {
        "id": 30702,
        "success": true
    }

Note: the response body from CoGe is passed back to the caller without modification.
