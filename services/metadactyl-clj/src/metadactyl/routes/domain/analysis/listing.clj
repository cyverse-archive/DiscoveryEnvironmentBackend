(ns metadactyl.routes.domain.analysis.listing
  (:use [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema optional-key Any Bool]])
  (:import [java.util UUID]))

(def Timestamp (describe String "A timestamp in milliseconds since the epoch."))

(defschema Analysis
  {(optional-key :app_description)
   (describe String "A description of the app used to perform the analysis.")

   :app_disabled
   (describe Boolean "Indicates whether the app is currently disabled.")

   :app_id
   (describe String "The ID of the app used to perform the analysis.")

   (optional-key :app_name)
   (describe String "The name of the app used to perform the analysis.")

   (optional-key :batch)
   (describe Boolean "Indicates whether the analysis is a batch analysis.")

   (optional-key :description)
   (describe String "The analysis description.")

   (optional-key :enddate)
   (describe Timestamp "The time the analysis ended.")

   :id
   (describe UUID "The analysis ID.")

   (optional-key :name)
   (describe String "The analysis name.")

   :notify
   (describe Boolean "Indicates whether the user wants status updates via email.")

   (optional-key :resultfolderid)
   (describe String "The path to the folder containing the anlaysis results.")

   (optional-key :startdate)
   (describe Timestamp "The time the analysis started.")

   :status
   (describe String "The status of the analysis.")

   :username
   (describe String "The name of the user who submitted the analysis.")

   (optional-key :wiki_url)
   (describe String "The URL to app documentation in Confluence.")})

(defschema AnalysisList
  {:analyses  (describe [Analysis] "The list of analyses.")
   :timestamp (describe Timestamp "The time the analysis list was retrieved.")
   :total     (describe Long "The total number of analyses in the result set.")})
