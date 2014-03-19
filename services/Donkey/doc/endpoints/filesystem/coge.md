Viewing a Genome File in CoGe
-----------------------------
A genome file may be submitted to CoGe for viewing within their genome viewer.
This endpoint will share the given genome files with the CoGe user, then submit those paths to their "genome load" service.
This service will return a URL where the authenticated user may view the genome viewer's progress, and the genome visualization once processing is done.

__URL Path__: /secured/coge/load-genomes

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
        "success": true,
        "coge_genome_url": "http://bit.ly/CoGeSample"
    }

__Curl Command__:

    curl -sd '{"paths":["/iplant/home/ipctest/simple.fasta"]}' http://127.0.0.1:3000/secured/coge/load-genomes?proxyToken=$(cas-ticket)

