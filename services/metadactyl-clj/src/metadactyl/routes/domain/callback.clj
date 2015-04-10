(ns metadactyl.routes.domain.callback
  (:use [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema optional-key]])
  (:import [java.util UUID]))

(defschema DeJobState
  {:status
   (describe String "The current status of the analysis")

   :uuid
   (describe String "The external identifier of the analysis")

   (optional-key :completion_date)
   (describe String "The analysis completion date as milliseconds since the epoch.")})

(defschema DeJobStatusUpdate
  {:state (describe DeJobState "The current state of the analysis.")})
