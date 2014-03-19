# Endpoints

All URLs referenced below are listed as relative URLs with value names enclosed
in braces. For example, the service to get a list of workflow elements is
accessed using the URL, `/get-workflow-elements/{element-type}`. Where
`{element-type}` refers to the type of workflow element that is being retrieved.
For example, to get a list of known property types, you can access the URL,
`/get-workflow-elements/property-types`. On the other hand, all examples use
fully qualified URLs.

Request and response bodies are in JSON format unless otherwise noted.

* [Miscellaneous Endpoints](endpoints/misc.md)
* [App Metadata Endpoints](endpoints/app-metadata.md)
    * [App Metadata Listing and Searching Services](endpoints/app-metadata/listing.md)
    * [App Categorization Services](endpoints/app-metadata/categorization.md)
    * [App Validation Services](endpoints/app-metadata/validation.md)
    * [App Information Services](endpoints/app-metadata/information.md)
    * [App Administration Services](endpoints/app-metadata/admin.md)
    * [App Rating Services](endpoints/app-metadata/rating.md)
    * [App Editing Services](endpoints/app-metadata/editing.md)
    * [Tool Request Services](endpoints/app-metadata/tool-requests.md)
    * [Updated App Administration Services](endpoints/app-metadata/updated-admin.md)
* [App Execution Endpoints](endpoints/app-execution.md)
* [Collaborator List Management Endpoints](endpoints/collaborators.md)
* [Reference Genome Endpoints](endpoints/reference-genomes.md)
