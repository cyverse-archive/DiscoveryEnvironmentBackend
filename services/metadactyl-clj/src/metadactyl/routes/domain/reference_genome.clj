(ns metadactyl.routes.domain.reference-genome
  (:use [common-swagger-api.schema :only [->optional-param describe]]
        [metadactyl.routes.params]
        [schema.core :only [defschema optional-key]])
  (:import [java.util Date UUID]))

(def ReferenceGenomeIdParam (describe UUID "A UUID that is used to identify the Reference Genome"))

(defschema ReferenceGenomeListingParams
  (merge SecuredQueryParams
    {(optional-key :deleted)
     (describe Boolean
       "Whether or not to include Reference Genomes that have been marked as deleted
        (false by default).")

     (optional-key :created_by)
     (describe String "Filters the Reference Genome listing by the user that added them.")}))

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

(defschema ReferenceGenomeSetRequest
  (-> ReferenceGenome
    (->optional-param :id)))

(defschema ReferenceGenomesSetRequest
  {:genomes (describe [ReferenceGenomeSetRequest] "Listing of Reference Genomes.")})

(defschema ReferenceGenomeRequest
  (-> ReferenceGenomeSetRequest
    (->optional-param :created_by)
    (->optional-param :last_modified_by)))
