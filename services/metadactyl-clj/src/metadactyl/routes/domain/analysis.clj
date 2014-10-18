(ns metadactyl.routes.domain.analysis
  (:use [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema optional-key Any Bool]])
  (:import [java.util UUID]))

(defschema AnalysisSubmission
  {:app_id
   (describe UUID "The UUID of the app used to perform the analysis.")

   (optional-key :callback)
   (describe String "The callback URL to use for job status updates.")

   :config
   (describe Any "A map from (str step-id \"_\" param-id) to param-value.")

   (optional-key :create_output_subdir)
   (describe Bool (str "Indicates whether a subdirectory should be created beneath "
                       "the specified output directory."))

   :debug
   (describe Bool "A flag indicating whether or not job debugging should be enabled.")

   :name
   (describe String "The name assigned to the analysis by the user.")

   :notify
   (describe Bool (str "Indicates whether the user wants to receive job status update "
                       "notifications."))

   (optional-key :description)
   (describe String "An optional description of the analysis.")

   :output_dir
   (describe String "The path to the analysis output directory in the data store.")

   (optional-key :uuid)
   (describe UUID (str "The UUID of the analysis. A random UUID will be assigned if one isn't "
                       "provided."))})

;; TODO: nuke this when it's no longer needed.
(def JexStepComponent (describe Any "this should be a schema"))

;; TODO: nuke this when it's no longer needed.
(def JexStepConfig (describe Any "this should be a schema"))

;; TODO: nuke this when it's no longer needed.
(defschema JexSubmissionStep
  {:component
   (describe JexStepComponent "The program used perform the analysis step.")

   :config
   (describe JexStepConfig "The configuration settings for an analysis step.")

   :environment
   (describe Any "A map of environment variable names to values.")

   :type
   (describe String "The type of the analysis step.")})

;; TODO: nuke this when it's no longer needed.
(defschema JexSubmission
  {:analysis_description
   (describe String "The app description from the database.")

   :analysis_id
   (describe UUID "The identifier of the app used in the submission.")

   :analysis_name
   (describe String "The name of the app used in the submission.")

   (optional-key :callback)
   (describe String "The callback URL to use for job status updates.")

   :create_output_subdir
   (describe Bool (str "Indicates whether a subdirectory should be created beneath the specified "
                       "output directory."))

   :description
   (describe String "A brief description of the analysis.")

   :email
   (describe String "The user's email address.")

   :execution_target
   (describe String "The execution system used the analsysis.")

   :name
   (describe String "The name of the analysis.")

   :notify
   (describe Bool (str "Indicates whether the user wants to receive job status update "
                       "notifications."))

   :output_dir
   (describe String "The path to the analysis output directory in the data store.")

   :request_type
   (describe String "The type of request being sent to the JEX.")

   (optional-key :starting_step)
   (describe Integer "The ordinal number of the step to start the job with.")

   :steps
   (describe [JexSubmissionStep] "The set of steps in the analysis.")

   :uuid
   (describe UUID "The UUID of the analysis.")

   :username
   (describe String "The username of the user who submitted the analysis.")})

(defschema SubmissionResponse
  {:id
   (describe UUID "The analysis identifier.")

   :name
   (describe String "The name assigned to the analysis by the user.")

   :status
   (describe String "The current status of the analysis.")

   :start_date
   (describe Long "The job submission time in milliseconds since the epoch.")})
