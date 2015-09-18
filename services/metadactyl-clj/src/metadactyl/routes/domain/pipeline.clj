(ns metadactyl.routes.domain.pipeline
  (:use [common-swagger-api.schema :only [->optional-param describe]]
        [metadactyl.routes.params]
        [metadactyl.routes.domain.app :only [AppTaskListing]]
        [schema.core :only [defschema optional-key Keyword]])
  (:import [java.util UUID]))

(defschema PipelineMappingMap
  {(describe Keyword "The input ID") (describe String "The output ID")})

(defschema PipelineMapping
  {:source_step (describe Long "The step index of the Source Step")
   :target_step (describe Long "The step index of the Target Step")
   :map (describe PipelineMappingMap "The {'input-id': 'output-id'} mappings")})

(defschema PipelineStep
  {:name
   (describe String "The Step's name")

   :description
   (describe String "The Step's description")

   (optional-key :task_id)
   (describe String "A String referring to either an internal task or an external app. If the
                     string refers to an internal task then this must be a string representation
                     of a UUID. Otherwise, it should be the ID of the external app.")

   (optional-key :external_app_id)
   (describe String "A string referring to an external app that is used to perform the step. This
                     field is required any time the task ID isn't provided.")

   :app_type
   (describe String "The Step's App type")})

(defschema Pipeline
  (merge AppTaskListing
    {:id
     (describe UUID "The pipeline's ID")

     :steps
     (describe [PipelineStep] "The Pipeline's steps")

     :mappings
     (describe [PipelineMapping] "The Pipeline's input/output mappings")}))

(defschema PipelineUpdateRequest
  (->optional-param Pipeline :tasks))

(defschema PipelineCreateRequest
  (->optional-param PipelineUpdateRequest :id))
