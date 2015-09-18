(ns metadactyl.routes.domain.analysis
  (:use [common-swagger-api.schema :only [describe]]
        [schema.core :only [defschema optional-key Any Bool]]
        [metadactyl.schema.containers :only [ToolContainer]])
  (:import [java.util UUID]))

(defschema ParameterValue
  {:value
   (describe Any "The value of the parameter.")})

(defschema AnalysisParameter
  {:full_param_id
   (describe String "The fully qualified parameter ID.")

   :param_id
   (describe String "The unqualified parameter ID.")

   (optional-key :param_name)
   (describe String "The name of the parameter.")

   (optional-key :param_value)
   (describe ParameterValue "The value of the parameter.")

   :param_type
   (describe String "The type of the parameter.")

   (optional-key :info_type)
   (describe String "The type of information associated with an input or output parameter.")

   (optional-key :data_format)
   (describe String "The data format associated with an input or output parameter.")

   (optional-key :is_default_value)
   (describe Bool "Indicates whether the default parameter value was used.")

   (optional-key :is_visible)
   (describe Bool "Indicates whether the parameter is visible in the app UI.")})

(defschema AnalysisParameters
  {:app_id     (describe String "The ID of the app used to perform the analysis.")
   :parameters (describe [AnalysisParameter] "The list of parameters.")})

(defschema AnalysisShredderRequest
  {:analyses (describe [UUID] "The identifiers of the analyses to be deleted.")})

(defschema StopAnalysisResponse
  {:id (describe UUID "the ID of the stopped analysis.")})

(defschema FileMetadata
  {:attr  (describe String "The attribute name.")
   :value (describe String "The attribute value.")
   :unit  (describe String "The attribute unit.")})

(defschema AnalysisSubmission
  {:app_id
   (describe String "The ID of the app used to perform the analysis.")

   (optional-key :job_id)
   (describe UUID "The UUID of the job being submitted.")

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
   (describe [FileMetadata] "Custom file attributes to associate with result files.")

   (optional-key :archive_logs)
   (describe Bool "True if the job logs should be uploaded to the data store.")})

(defschema AnalysisResponse
  {:id         (describe UUID "The ID of the submitted analysis.")
   :name       (describe String "The name of the submitted analysis.")
   :status     (describe String "The current status of the analysis.")
   :start-date (describe String "The analysis start date as milliseconds since the epoch.")})

(defschema JexStepComponent
  {(optional-key :description)
   (describe String "A brief description of the component.")

   (optional-key :container)
   ToolContainer

   :location
   (describe String "The path to the directory containing the component.")

   :name
   (describe String "The name of the executable file associated with the component.")

   (optional-key :type)
   (describe String "The type of the component.")})

(defschema JexStepInput
  {(optional-key :id)
   (describe UUID "The input ID.")

   (optional-key :multiplicity)
   (describe String "Indicates the number and organization of input files.")

   (optional-key :name)
   (describe String "The name of the input.")

   (optional-key :property)
   (describe String "The property value associated with the input.")

   (optional-key :retain)
   (describe Bool "True if the file should be retained in the analysis output folder.")

   (optional-key :type)
   (describe String "The type of data contained in the input.")

   (optional-key :value)
   (describe String "The path to the input in the data store.")})

(defschema JexStepOutput
  {(optional-key :multiplicity)
   (describe String "Indicates the number and organization of output files.")

   (optional-key :name)
   (describe String "The name of the output.")

   (optional-key :property)
   (describe String "The property value associated with the ouptut.")

   (optional-key :qual-id)
   (describe String "The fully qualified output ID.")

   (optional-key :retain)
   (describe Bool "True if the file should be retained in the analysis output folder.")

   (optional-key :type)
   (describe String "The type of data contained in the output.")})

(defschema JexStepParam
  {(optional-key :id)
   (describe UUID "The parameter ID.")

   (optional-key :name)
   (describe String "The command-line option associated with the parameter.")

   (optional-key :order)
   (describe Long "The relative ordering of the parameter on the command line.")

   (optional-key :value)
   (describe Any "The value to associate with the parameter.")})

(defschema JexStepConfig
  {:input
   (describe [JexStepInput] "The list of inputs for the job step.")

   :output
   (describe [JexStepOutput] "The list of outputs for the job step.")

   :params
   (describe [JexStepParam] "The list of parameters for the job step.")})

(defschema JexSubmissionStep
  {:component
   (describe JexStepComponent "The program used perform the analysis step.")

   :config
   (describe JexStepConfig "The configuration settings for an analysis step.")

   :environment
   (describe Any "A map of environment variable names to values.")

   (optional-key :stdout)
   (describe String
     "A path relative to the current working directory where stdout should be redirected.")

   (optional-key :stderr)
   (describe String
     "A path relative to the current working directory where stderr should be redirected.")

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
