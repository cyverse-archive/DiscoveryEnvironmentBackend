(ns metadactyl.routes.domain.reference-genome
  (:use [metadactyl.routes.params]
        [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema optional-key]])
  (:import [java.util Date UUID]))

(def ReferenceGenomeIdParam (describe UUID "A UUID that is used to identify the Reference Genome"))

(defschema ReferenceGenome
  {:id
   ReferenceGenomeIdParam

   :name
   (describe String "The Reference Genome's name")

   :path
   (describe String "The path of the directory containing the Reference Genome")

   (optional-key :deleted)
   (describe Boolean "Whether the Reference Genome is marked as deleted")

   :created_by
   (describe String "The username of the user that added the Reference Genome")

   (optional-key :created_on)
   (describe Date "The date the Reference Genome was added")

   :last_modified_by
   (describe String "The username of the user that updated the Reference Genome")

   (optional-key :last_modified_on)
   (describe Date "The date of last modification to the Reference Genome")})

(defschema ReferenceGenomesList
  {:genomes (describe [ReferenceGenome] "Listing of Reference Genomes.")})

(defschema ReferenceGenomeRequest
  (-> ReferenceGenome
    (->optional-param :id)))

(defschema ReferenceGenomesSetRequest
  {:genomes (describe [ReferenceGenomeRequest] "Listing of Reference Genomes.")})
