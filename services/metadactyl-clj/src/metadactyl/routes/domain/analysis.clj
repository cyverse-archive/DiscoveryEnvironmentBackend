(ns metadactyl.routes.domain.analysis
  (:use [ring.swagger.schema :only [describe]]
        [schema.core :only [defschema optional-key Any Bool]])
  (:import [java.util UUID]))

(defschema FileMetadata
  {:attr  (describe String "The attribute name.")
   :value (describe String "The attribute value.")
   :unit  (describe String "The attribute unit.")})

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

   (optional-key :description)
   (describe String "An optional description of the analysis.")

   :name
   (describe String "The name assigned to the analysis by the user.")

   :notify
   (describe Bool (str "Indicates whether the user wants to receive job status update "
                       "notifications."))

   :output_dir
   (describe String "The path to the analysis output directory in the data store.")

   (optional-key :starting_step)
   (describe Long "The ordinal number of the step to start the job with.")

   (optional-key :uuid)
   (describe UUID (str "The UUID of the analysis. A random UUID will be assigned if one isn't "
                       "provided."))

   (optional-key :skip-parent-meta)
   (describe Bool "True if metadata should not associate metadata with the parent directory.")

   (optional-key :file-metadata)
   (describe [FileMetadata] "Custom file attributes to associate with result files.")})

(def JexStepComponent (describe Any "this should be a schema"))

(def JexStepConfig (describe Any "this should be a schema"))

(defschema JexSubmissionStep
  {:component
   (describe JexStepComponent "The program used perform the analysis step.")

   :config
   (describe JexStepConfig "The configuration settings for an analysis step.")

   :environment
   (describe Any "A map of environment variable names to values.")

   :type
   (describe String "The type of the analysis step.")})

(defschema JexSubmission
  {:app_description
   (describe String "The app description from the database.")

   :app_id
   (describe UUID "The identifier of the app used in the submission.")

   :app_name
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
   (describe Long "The ordinal number of the step to start the job with.")

   :steps
   (describe [JexSubmissionStep] "The set of steps in the analysis.")

   :uuid
   (describe UUID "The UUID of the analysis.")

   :username
   (describe String "The username of the user who submitted the analysis.")

   (optional-key :wiki_url)
   (describe String "A link to the app's documentation page.")

   (optional-key :skip-parent-meta)
   (describe Bool "True if metadata should not associate metadata with the parent directory.")

   (optional-key :file-metadata)
   (describe [FileMetadata] "Custom file attributes to associate with result files.")})

(defschema SubmissionResponse
  {:id
   (describe UUID "The analysis identifier.")

   :name
   (describe String "The name assigned to the analysis by the user.")

   :status
   (describe String "The current status of the analysis.")

   :start_date
   (describe Long "The job submission time in milliseconds since the epoch.")})
