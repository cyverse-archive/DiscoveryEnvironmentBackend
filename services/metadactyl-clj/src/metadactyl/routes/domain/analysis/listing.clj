(ns metadactyl.routes.domain.analysis.listing
  (:use [common-swagger-api.schema :only [describe]]
        [metadactyl.routes.params :only [ResultsTotalParam]]
        [schema.core :only [defschema optional-key Any Int Bool]])
  (:import [java.util UUID]))

(def Timestamp (describe String "A timestamp in milliseconds since the epoch."))

(defschema BatchStatus
  {:total     (describe Int "The total number of jobs in the batch.")
   :completed (describe Int "The number of completed jobs in the batch.")
   :running   (describe Int "The number of running jobs in the batch.")
   :submitted (describe Int "The number of submitted jobs in the batch.")})

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
   (describe String "The URL to app documentation in Confluence.")

   (optional-key :parent_id)
   (describe UUID "The identifier of the parent analysis.")

   (optional-key :batch_status)
   (describe BatchStatus "A summary of the status of the batch.")})

(defschema AnalysisList
  {:analyses  (describe [Analysis] "The list of analyses.")
   :timestamp (describe Timestamp "The time the analysis list was retrieved.")
   :total     ResultsTotalParam})

(defschema AnalysisUpdate
  (select-keys Analysis (map optional-key [:description :name])))

(defschema AnalysisUpdateResponse
  (select-keys Analysis (cons :id (map optional-key [:description :name]))))

(def AppStepNumber
  (describe Integer (str "The sequential step number from the app, which might be different "
                         "from the analysis step number if app steps have been combined.")))

(defschema AnalysisStep
  {:step_number
   (describe Integer "The sequential step number in the analysis.")

   (optional-key :external_id)
   (describe String "The step ID from the execution system.")

   (optional-key :startdate)
   (describe Timestamp "The time the step started.")

   (optional-key :enddate)
   (describe Timestamp "The time the step ended.")

   (optional-key :status)
   (describe String "The status of the step.")

   (optional-key :app_step_number)
   AppStepNumber

   (optional-key :step_type)
   (describe String "The analysis type associated with the step.")})

(defschema AnalysisStepList
  {:analysis_id (describe UUID "The analysis ID.")
   :steps       (describe [AnalysisStep] "The list of analysis steps.")
   :timestamp   (describe Timestamp "The time the list of analysis steps was retrieved.")
   :total       ResultsTotalParam})
