(ns metadactyl.routes.reference-genomes
  (:use [metadactyl.metadata.reference-genomes :only [list-reference-genomes]]
        [metadactyl.routes.domain.reference-genome]
        [metadactyl.routes.params]
        [compojure.api.sweet])
  (:require [clojure-commons.error-codes :as ce]))

(defroutes* reference-genomes
  (GET* "/" [:as {uri :uri}]
        :query [params SecuredQueryParams]
        :return ReferenceGenomesList
        :summary "List Reference Genomes."
        :notes "This endpoint may be used to obtain lists of all available Reference Genomes."
        (ce/trap uri #(list-reference-genomes))))
