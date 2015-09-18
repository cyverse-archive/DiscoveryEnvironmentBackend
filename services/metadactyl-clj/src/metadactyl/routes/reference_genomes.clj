(ns metadactyl.routes.reference-genomes
  (:use [common-swagger-api.schema]
        [metadactyl.metadata.reference-genomes :only [get-reference-genome list-reference-genomes]]
        [metadactyl.routes.domain.reference-genome]
        [metadactyl.routes.params])
  (:require [clojure-commons.error-codes :as ce]))

(defroutes* reference-genomes
  (GET* "/" [:as {uri :uri}]
        :query [params ReferenceGenomeListingParams]
        :return ReferenceGenomesList
        :summary "List Reference Genomes."
        :description "This endpoint may be used to obtain lists of all available Reference Genomes."
        (ce/trap uri #(list-reference-genomes params)))

  (GET* "/:reference-genome-id" [:as {uri :uri}]
        :path-params [reference-genome-id :- ReferenceGenomeIdParam]
        :query [params SecuredQueryParams]
        :return ReferenceGenome
        :summary "Get a Reference Genome."
        :description "This endpoint may be used to obtain a Reference Genome by its UUID."
        (ce/trap uri #(get-reference-genome reference-genome-id))))
