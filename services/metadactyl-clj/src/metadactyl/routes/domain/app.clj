(ns metadactyl.routes.domain.app
  (:use [common-swagger-api.schema :only [->optional-param describe]]
        [metadactyl.routes.params]
        [metadactyl.routes.domain.app.rating]
        [metadactyl.routes.domain.tool :only [Tool]]
        [schema.core :only [Any defschema optional-key recursive]])
  (:import [java.util UUID Date]))

(def AppIdParam (describe UUID "A UUID that is used to identify the App"))
(def StringAppIdParam (describe String "The App identifier."))
(def OptionalIdParam (describe UUID "An optional UUID identifier"))
(def AppDocParam (describe String "The App's documentation"))
(def AppDocUrlParam (describe String "The App's documentation URL"))
(def AppReferencesParam (describe [String] "The App's references"))
(def AppDeletedParam (describe Boolean "Whether the App is marked as deleted"))
(def AppDisabledParam (describe Boolean "Whether the App is marked as disabled"))
(def AppPublicParam (describe Boolean
                      "Whether the App has been published and is viewable by all users"))

(def OptionalGroupsKey (optional-key :groups))
(def OptionalParametersKey (optional-key :parameters))
(def OptionalParameterArgumentsKey (optional-key :arguments))

(def ToolListDocs "The tools used to execute the App")
(def GroupListDocs "The list of Parameter Groups associated with the App")
(def ParameterListDocs "The list of Parameters in this Group")
(def ListItemOrTreeDocs
  "The List Parameter's arguments. Only used in cases where the user is given a fixed number of
   values to choose from. This can occur for Parameters such as `TextSelection` or
   `IntegerSelection` Parameters")
(def TreeSelectorParameterListDocs "The TreeSelector root's arguments")
(def TreeSelectorGroupListDocs "The TreeSelector root's groups")
(def TreeSelectorGroupParameterListDocs "The TreeSelector Group's arguments")
(def TreeSelectorGroupGroupListDocs "The TreeSelector Group's groups")

(defschema AppParameterListItem
  {:id                         (describe UUID "A UUID that is used to identify the List Item")
   (optional-key :name)        (describe String "The List Item's name")
   (optional-key :value)       (describe String "The List Item's value")
   (optional-key :description) (describe String "The List Item's description")
   (optional-key :display)     (describe String "The List Item's display label")
   (optional-key :isDefault)   (describe Boolean "Flags this Item as the List's default selection")})

(defschema AppParameterListGroup
  (merge AppParameterListItem
         {OptionalParameterArgumentsKey
          (describe [AppParameterListItem] TreeSelectorGroupParameterListDocs)

          OptionalGroupsKey
          (describe [(recursive #'AppParameterListGroup)] TreeSelectorGroupGroupListDocs)}))

(defschema AppParameterListItemOrTree
  (merge AppParameterListItem
         {(optional-key :isSingleSelect)
          (describe Boolean "The TreeSelector root's single-selection flag")

          (optional-key :selectionCascade)
          (describe String "The TreeSelector root's cascace option")

          OptionalParameterArgumentsKey
          (describe [AppParameterListItem] TreeSelectorParameterListDocs)

          OptionalGroupsKey
          (describe [AppParameterListGroup] TreeSelectorGroupListDocs)}))

(defschema AppParameterValidator
  {:type
   (describe String
     "The validation rule's type, which describes how a property value should be validated. For
      example, if the type is `IntAbove` then the property value entered by the user must be an
      integer above a specific value, which is specified in the parameter list. You can use the
      `rule-types` endpoint to get a list of validation rule types")

   :params
   (describe [Any]
     "The list of parameters to use when validating a Parameter value. For example, to ensure that a
      Parameter contains a value that is an integer greater than zero, you would use a validation
      rule of type `IntAbove` along with a parameter list of `[0]`")})

(defschema AppFileParameters
  {(optional-key :format)
   (describe String "The Input/Output Parameter's file format")

   (optional-key :file_info_type)
   (describe String "The Input/Output Parameter's info type")

   (optional-key :is_implicit)
   (describe Boolean
     "Whether the Output Parameter name is specified on the command line (but still be referenced in
      Pipelines), or implicitly determined by the app itself. If the output file name is implicit
      then the output file name either must always be the same or it must follow a naming convention
      that can easily be matched with a glob pattern")

   (optional-key :repeat_option_flag)
   (describe Boolean
     "Whether or not the command-line option flag should preceed each file of a MultiFileSelector
     on the command line when the App is run")

   (optional-key :data_source)
   (describe String "The Output Parameter's source")

   (optional-key :retain)
   (describe Boolean
     "Whether or not the Input should be copied back to the job output directory in iRODS")})

(defschema AppParameter
  {:id
   (describe UUID "A UUID that is used to identify the Parameter")

   (optional-key :name)
   (describe String
     "The Parameter's name. In most cases, this field indicates the command-line option used to
      identify the Parameter on the command line. In these cases, the Parameter is assumed to be
      positional and no command-line option is used if the name is blank. For Parameters that
      specify a limited set of selection values, however, this is not the case. Instead, the
      Parameter arguments specify both the command-line flag and the Parameter value to use for each
      option that is selected")

   (optional-key :defaultValue)
   (describe Any "The Parameter's default value")

   (optional-key :value)
   (describe Any "The Parameter's value, used for previewing this parameter on the command-line.")

   (optional-key :label)
   (describe String "The Parameter's prompt to display in the UI")

   (optional-key :description)
   (describe String "The Parameter's description")

   (optional-key :order)
   (describe Long
     "The relative command-line order for the Parameter. If this field is not specified then the
      arguments will appear on the command-line in the order in which they appear in the import JSON.
      If you're not specifying the order, please be sure that the argument order is unimportant for
      the tool being integrated")

   (optional-key :required)
   (describe Boolean "Whether or not a value is required for this Parameter")

   (optional-key :isVisible)
   (describe Boolean "The Parameter's intended visibility in the job submission UI")

   (optional-key :omit_if_blank)
   (describe Boolean
     "Whether the command-line option should be omitted if the Parameter value is blank. This is
      most useful for optional arguments that use command-line flags in conjunction with a value. In
      this case, it is an error to include the command-line flag without a corresponding value. This
      flag indicates that the command-line flag should be omitted if the value is blank. This can
      also be used for positional arguments, but this flag tends to be useful only for trailing
      positional arguments")

   :type
   (describe String
     "The Parameter's type name. Must contain the name of one of the Parameter types defined in the
      database. You can get the list of defined and undeprecated Parameter types using the
      `parameter-types` endpoint")

   (optional-key :file_parameters)
   (describe AppFileParameters "The File Parameter specific details")

   OptionalParameterArgumentsKey
   (describe [AppParameterListItemOrTree] ListItemOrTreeDocs)

   (optional-key :validators)
   (describe [AppParameterValidator]
     "The Parameter's validation rules, which contains a list of rules that can be used to verify
      that Parameter values entered by a user are valid. Note that in cases where the user is given
      a list of possibilities to choose from, no validation rules are required because the selection
      list itself can be used to validate the Parameter value")})

(defschema AppGroup
  {:id
   (describe UUID "A UUID that is used to identify the Parameter Group")

   (optional-key :name)
   (describe String "The Parameter Group's name")

   (optional-key :description)
   (describe String "The Parameter Group's description")

   :label
   (describe String "The label used to identify the Parameter Group in the UI")

   (optional-key :isVisible)
   (describe Boolean "The Parameter Group's intended visibility in the job submission UI")

   OptionalParametersKey
   (describe [AppParameter] ParameterListDocs)})

(defschema AppBase
  {:id                              AppIdParam
   :name                            (describe String "The App's name")
   :description                     (describe String "The App's description")
   (optional-key :integration_date) (describe Date "The App's Date of public submission")
   (optional-key :edited_date)      (describe Date "The App's Date of its last edit")})

(defschema App
  (merge AppBase
         {(optional-key :tools)      (describe [Tool] ToolListDocs)
          (optional-key :references) AppReferencesParam
          OptionalGroupsKey          (describe [AppGroup] GroupListDocs)}))

(defschema AppFileParameterDetails
  {:id          (describe String "The Parameter's ID")
   :name        (describe String "The Parameter's name")
   :description (describe String "The Parameter's description")
   :label       (describe String "The Input Parameter's label or the Output Parameter's value")
   :format      (describe String "The Parameter's file format")
   :required    (describe Boolean "Whether or not a value is required for this Parameter")})

(defschema AppTask
  {:id          (describe String "The Task's ID")
   :name        (describe String "The Task's name")
   :description (describe String "The Task's description")
   :inputs      (describe [AppFileParameterDetails] "The Task's input parameters")
   :outputs     (describe [AppFileParameterDetails] "The Task's output parameters")})

(defschema AppTaskListing
  (assoc AppBase
    :id    (describe String "The App's ID.")
    :tasks (describe [AppTask] "The App's tasks")))

(defschema AppParameterJobView
  (assoc AppParameter
    :id
    (describe String
      "A string consisting of the App's step ID and the Parameter ID separated by an underscore.
       Both identifiers are necessary because the same task may be associated with a single App,
       which would cause duplicate keys in the job submission JSON. The step ID is prepended to
       the Parameter ID in order to ensure that all parameter value keys are unique.")))

(defschema AppGroupJobView
  (assoc AppGroup
    :id                   (describe String "The app group ID.")
    :step_number          (describe Long "The step number associated with this parameter group")
    OptionalParametersKey (describe [AppParameterJobView] ParameterListDocs)))

(defschema AppJobView
  (assoc AppBase
    :app_type         (describe String "DE or External.")
    :id               (describe String "The app ID.")
    :label            (describe String "An alias for the App's name")
    :deleted          AppDeletedParam
    :disabled         AppDisabledParam
    OptionalGroupsKey (describe [AppGroupJobView] GroupListDocs)))

(defschema AppDetailCategory
  {:id AppCategoryIdPathParam
   :name (describe String "The App Category's name")})

(defschema AppDetailsTool
  (assoc Tool
    :id (describe String "The tool identifier.")))

(defschema AppDetails
  (merge AppBase
         {:id
          (describe String "The app identifier.")

          :tools
          (describe [AppDetailsTool] ToolListDocs)

          :deleted
          AppDeletedParam

          :disabled
          AppDisabledParam

          :integrator_email
          (describe String "The App integrator's email address.")

          :integrator_name
          (describe String "The App integrator's full name.")

          (optional-key :wiki_url)
          AppDocUrlParam

          :references
          AppReferencesParam

          :categories
          (describe [AppDetailCategory]
            "The list of Categories associated with the App")

          :suggested_categories
          (describe [AppDetailCategory]
            "The list of Categories the integrator wishes to associate with the App")}))

(defschema AppDocumentation
  {(optional-key :app_id)
   StringAppIdParam

   :documentation
   AppDocParam

   :references
   AppReferencesParam

   (optional-key :created_on)
   (describe Date "The Date the App's documentation was created")

   (optional-key :modified_on)
   (describe Date "The Date the App's documentation was last modified")

   (optional-key :created_by)
   (describe String "The user that created the App's documentation")

   (optional-key :modified_by)
   (describe String "The user that last modified the App's documentation")})

(defschema AppDocumentationRequest
  (dissoc AppDocumentation :references))

(defschema PipelineEligibility
  {:is_valid (describe Boolean "Whether the App can be used in a Pipeline")
   :reason (describe String "The reason an App cannot be used in a Pipeline")})

(defschema AppListingDetail
  (merge AppBase
    {:id
     (describe String "The app ID.")

     :app_type
     (describe String "The type ID of the App")

     :can_favor
     (describe Boolean "Whether the current user can favorite this App")

     :can_rate
     (describe Boolean "Whether the current user can rate this App")

     :can_run
     (describe Boolean
       "This flag is calculated by comparing the number of steps in the app to the number of steps
        that have a tool associated with them. If the numbers are different then this flag is set to
        `false`. The idea is that every step in the analysis has to have, at the very least, a tool
        associated with it in order to run successfully")

     :deleted
     AppDeletedParam

     :disabled
     AppDisabledParam

     :integrator_email
     (describe String "The App integrator's email address")

     :integrator_name
     (describe String "The App integrator's full name")

     (optional-key :is_favorite)
     (describe Boolean "Whether the current user has marked the App as a favorite")

     :is_public
     AppPublicParam

     :pipeline_eligibility
     (describe PipelineEligibility "Whether the App can be used in a Pipeline")

     :rating
     (describe Rating "The App's rating details")

     :step_count
     (describe Long "The number of Tasks this App executes")

     (optional-key :wiki_url)
     AppDocUrlParam}))

(defschema AppListing
  {:app_count (describe Long "The total number of Apps in the listing")
   :apps      (describe [AppListingDetail] "A listing of App details")})

(defschema AppIdList
  {:app_ids (describe [UUID] "A List of UUIDs used to identify Apps")})

(defschema AppDeletionRequest
  (merge AppIdList
         {(optional-key :root_deletion_request)
          (describe Boolean "Set to `true` to  delete one or more public apps")}))

(defschema AppParameterListItemRequest
  (->optional-param AppParameterListItem :id))

(defschema AppParameterListGroupRequest
  (-> AppParameterListGroup
    (->optional-param :id)
    (assoc OptionalParameterArgumentsKey
           (describe [AppParameterListItemRequest] TreeSelectorGroupParameterListDocs)
           OptionalGroupsKey
           (describe [(recursive #'AppParameterListGroupRequest)] TreeSelectorGroupGroupListDocs))))

(defschema AppParameterListItemOrTreeRequest
  (-> AppParameterListItemOrTree
    (->optional-param :id)
    (assoc OptionalParameterArgumentsKey
           (describe [AppParameterListItemRequest] TreeSelectorParameterListDocs))
    (assoc OptionalGroupsKey
           (describe [AppParameterListGroupRequest] TreeSelectorGroupListDocs))))

(defschema AppParameterRequest
  (-> AppParameter
    (->optional-param :id)
    (assoc OptionalParameterArgumentsKey
           (describe [AppParameterListItemOrTreeRequest] ListItemOrTreeDocs))))

(defschema AppGroupRequest
  (-> AppGroup
      (->optional-param :id)
      (assoc OptionalParametersKey (describe [AppParameterRequest] ParameterListDocs))))

(defschema AppRequest
  (-> App
    (->optional-param :id)
    (assoc OptionalGroupsKey (describe [AppGroupRequest] GroupListDocs))))

(defschema AppPreviewRequest
  (-> App
    (->optional-param :id)
    (->optional-param :name)
    (->optional-param :description)
    (assoc OptionalGroupsKey (describe [AppGroupRequest] GroupListDocs)
           (optional-key :is_public) AppPublicParam)))

(defschema AppCategoryIdListing
  {:categories (describe [UUID] "A listing of App Category IDs")})

(defschema PublishAppRequest
  (-> AppBase
    (->optional-param :id)
    (->optional-param :name)
    (->optional-param :description)
    (assoc :documentation AppDocParam
           :references AppReferencesParam)
    (merge AppCategoryIdListing)))

(defschema AdminAppPatchRequest
  (-> AppBase
    (->optional-param :id)
    (->optional-param :name)
    (->optional-param :description)
    (assoc (optional-key :wiki_url)   AppDocUrlParam
           (optional-key :references) AppReferencesParam
           (optional-key :deleted)    AppDeletedParam
           (optional-key :disabled)   AppDisabledParam
           OptionalGroupsKey          (describe [AppGroup] GroupListDocs))))
