(ns metadactyl.metadactyl
  (:use [clojure.java.io :only [reader]]
        [kameleon.queries :only [record-login record-logout]]
        [metadactyl.app-validation]
        [metadactyl.beans]
        [metadactyl.util.config]
        [metadactyl.metadata.analyses :only [get-selected-analyses]]
        [metadactyl.metadata.reference-genomes :only [get-reference-genomes put-reference-genomes]]
        [metadactyl.metadata.element-listings :only [list-elements]]
        [metadactyl.util.service]
        [metadactyl.transformers]
        [metadactyl.user :only [current-user]]
        [metadactyl.validation :only [validate-json-array-field]]
        [ring.util.codec :only [url-decode]]
        [slingshot.slingshot :only [throw+ try+]])
  (:import [com.mchange.v2.c3p0 ComboPooledDataSource]
           [java.util HashMap]
           [org.iplantc.authn.service UserSessionService]
           [org.iplantc.authn.user User]
           [org.iplantc.workflow.client OsmClient]
           [org.iplantc.workflow.experiment ExperimentRunner IrodsUrlAssembler]
           [org.iplantc.workflow.integration.validation
            ChainingTemplateValidator OutputRedirectionTemplateValidator
            TemplateValidator]
           [org.iplantc.workflow.service
            AnalysisCategorizationService AnalysisEditService CategoryService
            ExportService InjectableWorkspaceInitializer PipelineService
            TemplateGroupService UserService WorkflowExportService
            AnalysisListingService WorkflowPreviewService WorkflowImportService
            RatingService WorkflowElementSearchService PropertyValueService
            UiAnalysisService]
           [org.springframework.orm.hibernate3.annotation
            AnnotationSessionFactoryBean])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.translations.app-metadata :as app-meta-tx]
            [metadactyl.translations.property-values :as prop-value-tx]
            [metadactyl.workspace :as ws]))

(defn- get-property-type-validator
  "Gets an implementation of the TemplateValidator interface that can be used
   to verify that the property types in the templates are all compatible with
   the selected deployed component."
  []
  (proxy [TemplateValidator] []
    (validate [template registry]
      (validate-template-property-types template registry))))

(defn- get-deployed-component-validator
  "Gets an implementation of the TemplateValidator interface that can be used
   to verify that a valid deployed component is associated with the template."
  []
  (proxy [TemplateValidator] []
    (validate [template registry]
      (validate-template-deployed-component template))))

(def
  ^{:doc "The service used to get information about the authenticated user."}
   user-session-service (proxy [UserSessionService] []
                          (getUser [] current-user)))

(defn- build-private-template-validator
  "Builds an object that will be used by the workflow import services
   to validate incoming templates."
  []
  (doto (ChainingTemplateValidator.)
    (.addValidator (OutputRedirectionTemplateValidator. "stdout"))
    (.addValidator (OutputRedirectionTemplateValidator. "stderr"))
    (.addValidator (get-property-type-validator))))

(defn- build-public-template-validator
  "Builds an object that will be used by the make-analysis-public service to
   validate applications that are being made public."
  []
  (doto (ChainingTemplateValidator.)
    (.addValidator (get-deployed-component-validator))
    (.addValidator (build-private-template-validator))))

(defn user-from-attributes
  "Creates an instance of org.iplantc.authn.user.User from the given map."
  [user-attributes]
  (log/debug user-attributes)
  (let [uid (user-attributes :user)]
    (if (empty? uid)
      (throw+ {:type :metadactyl.util.service/unauthorized,
               :user user-attributes,
               :message "Invalid user credentials provided."}))
    (doto (User.)
      (.setUsername      (str uid "@" (uid-domain)))
      (.setPassword      (:password user-attributes ""))
      (.setEmail         (:email user-attributes ""))
      (.setShortUsername uid)
      (.setFirstName     (:first-name user-attributes ""))
      (.setLastName      (:last-name user-attributes "")))))

(defmacro with-user
  "Performs a task with the given user information bound to current-user."
  [[user] & body]
  `(binding [current-user (user-from-attributes ~user)]
     (do ~@body)))

(defn store-current-user
  "Creates a function that takes a request, binds current-user to a new instance
   of org.iplantc.authn.user.User that is built from the user attributes found
   in the given params map, then passes request to the given handler."
  [handler params]
  (fn [request]
    (trap #(with-user [params] (handler request)))))

(register-bean
  (defbean db-url
    "The URL to use when connecting to the database."
    (str "jdbc:" (db-subprotocol) "://" (db-host) ":" (db-port) "/" (db-name))))

(register-bean
  (defbean data-source
    "The data source used to obtain database connections."
    (doto (ComboPooledDataSource.)
      (.setDriverClass (db-driver-class))
      (.setJdbcUrl (db-url))
      (.setUser (db-user))
      (.setPassword (db-password)))))

(register-bean
  (defbean session-factory
    "A factory for generating Hibernate sessions."
    (.getObject
      (doto (AnnotationSessionFactoryBean.)
        (.setDataSource (data-source))
        (.setPackagesToScan (into-array String (hibernate-packages)))
        (.setMappingResources (into-array String (hibernate-resources)))
        (.setHibernateProperties (as-properties
                                   {"hibernate.dialect" (hibernate-dialect)
                                    "hibernate.hbm2ddl.auto" "validate"
                                    "hibernate.jdbc.batch-size" "50"}))
        (.afterPropertiesSet)))))

(register-bean
  (defbean workflow-element-search-service
    "Services used to search elements that are commonly shared by multiple
     apps in the metadata model (currently, only deployed components)."
    (doto (WorkflowElementSearchService.)
      (.setSessionFactory (session-factory)))))

(register-bean
  (defbean workflow-export-service
    "Services used to export apps and templates from the DE."
    (WorkflowExportService. (session-factory))))

(register-bean
  (defbean export-service
    "Services used to determine whether or not an ap can be exported."
    (doto (ExportService.)
      (.setSessionFactory (session-factory)))))

(register-bean
  (defbean category-service
    "Services used to manage app categories."
    (doto (CategoryService.)
      (.setSessionFactory (session-factory)))))

(register-bean
  (defbean pipeline-service
    "Services used to manage pipelines"
    (doto (PipelineService.)
      (.setSessionFactory (session-factory)))))

(register-bean
  (defbean osm-job-request-client
    "The client used to communicate with OSM services."
    (doto (OsmClient.)
      (.setBaseUrl (osm-base-url))
      (.setBucket (osm-job-request-bucket))
      (.setConnectionTimeout (osm-connection-timeout))
      (.setEncoding (osm-encoding)))))

(register-bean
  (defbean user-service
    "Services used to obtain information about a user."
    (doto (UserService.)
      (.setSessionFactory (session-factory))
      (.setUserSessionService user-session-service)
      (.setRootAnalysisGroup (workspace-root-app-group))
      (.setDefaultAnalysisGroups (workspace-default-app-groups)))))

(register-bean
  (defbean workspace-initializer
    "A bean that can be used to initialize a user's workspace."
    (doto (InjectableWorkspaceInitializer.)
      (.setUserService (user-service)))))

(register-bean
  (defbean analysis-categorization-service
    "Services used to categorize apps."
    (doto (AnalysisCategorizationService.)
      (.setSessionFactory (session-factory))
      (.setDevAnalysisGroupIndex (workspace-dev-app-group-index))
      (.setFavoritesAnalysisGroupIndex (workspace-favorites-app-group-index))
      (.setWorkspaceInitializer (workspace-initializer)))))

(register-bean
  (defbean analysis-listing-service
    "Services used to list analyses."
    (doto (AnalysisListingService.)
      (.setSessionFactory (session-factory))
      (.setFavoritesAnalysisGroupIndex (workspace-favorites-app-group-index))
      (.setWorkspaceInitializer (workspace-initializer)))))

(register-bean
  (defbean template-group-service
    "Services used to place apps in app groups."
    (doto (TemplateGroupService.)
      (.setSessionFactory (session-factory))
      (.setUserSessionService user-session-service)
      (.setTemplateValidator (build-public-template-validator)))))

(register-bean
  (defbean workflow-preview-service
    "Handles workflow/metadactyl related previews."
    (WorkflowPreviewService. (session-factory))))

(register-bean
 (defbean workflow-import-service
   "Handles workflow/metadactyl import actions."
   (doto (WorkflowImportService.
          (session-factory)
          (Integer/toString (workspace-dev-app-group-index))
          (Integer/toString (workspace-favorites-app-group-index))
          (workspace-initializer))
     (.setTemplateValidator (build-private-template-validator)))))

(register-bean
  (defbean analysis-edit-service
    "Services to make apps available for editing in Tito."
    (doto (AnalysisEditService.)
      (.setSessionFactory (session-factory))
      (.setUserService (user-service))
      (.setWorkflowImportService (workflow-import-service)))))

(register-bean
  (defbean rating-service
    "Services to associate user ratings with or remove user ratings from
     apps."
    (doto (RatingService.)
      (.setSessionFactory (session-factory))
      (.setUserSessionService user-session-service))))

(register-bean
  (defbean url-assembler
    "Used to assemble URLs."
    (IrodsUrlAssembler.)))

(register-bean
  (defbean experiment-runner
    "Services to submit jobs to the JEX for execution."
    (doto (ExperimentRunner.)
      (.setSessionFactory (session-factory))
      (.setUserService (user-service))
      (.setExecutionUrl (jex-base-url))
      (.setUrlAssembler (url-assembler))
      (.setJobRequestOsmClient (osm-job-request-client))
      (.setIrodsHome (string/replace (irods-home) #"/$" "")))))

(register-bean
  (defbean property-value-service
    "Services to retrieve property values for jobs that have previously been
     submitted."
    (doto (PropertyValueService.)
      (.setSessionFactory (session-factory))
      (.setOsmClient (osm-job-request-client)))))

(register-bean
 (defbean ui-app-service
   "Services to retrieve apps in the format expected by the UI."
   (doto (UiAnalysisService.)
     (.setSessionFactory (session-factory)))))

(defn get-workflow-elements
  "A service to get information about workflow elements."
  [element-type params]
  (success-response (list-elements element-type params)))

(defn search-deployed-components
  "A service to search information about deployed components."
  [search-term]
  (.searchDeployedComponents (workflow-element-search-service) search-term))

(defn delete-categories
  "A service used to delete app categories."
  [body]
  (.deleteCategories (category-service) (slurp body)))

(defn validate-app-for-pipelines
  "A service used to determine whether or not an app can be included in a
   pipeline."
  [app-id]
  (.validateAnalysisForPipelines (pipeline-service) app-id))

(defn get-data-objects-for-app
  "A service used to list the data objects in an app."
  [app-id]
  (.getDataObjectsForAnalysis (pipeline-service) app-id))

(defn get-app-categories
  "A service used to get a list of app categories."
  [category-set]
  (.getAnalysisCategories (analysis-categorization-service) category-set))

(defn can-export-app
  "A service used to determine whether or not an app can be exported to Tito."
  [body]
  (.canExportAnalysis (export-service) (slurp body)))

(defn add-app-to-group
  "A service used to add an existing app to an app group."
  [body]
  (.addAnalysisToTemplateGroup (template-group-service) (slurp body)))

(defn get-app
  "A service used to get an app in the format required by the DE."
  [app-id]
  (.getAnalysis (ui-app-service) app-id))

(defn get-app-new-format
  "This service will retrieve an app in the format required by the DE as of version 1.8."
  [app-id]
  (-> (get-app app-id)
      (cheshire/decode true)
      (app-meta-tx/template-internal-to-external)
      (cheshire/encode)))

(defn export-template
  "This service will export the template with the given identifier."
  [template-id]
  (.exportTemplate (workflow-export-service) template-id))

(defn export-workflow
  "This service will export a workflow with the given identifier."
  [app-id]
  (.exportAnalysis (workflow-export-service) app-id))

(defn export-deployed-components
  "This service will export all or selected deployed components."
  [body]
  (.getDeployedComponents (workflow-export-service) (slurp body)))

(defn preview-template
  "This service will convert a JSON document in the format consumed by
   the import service into the format required by the DE."
  [body]
  (.previewTemplate (workflow-preview-service) (slurp body)))

(defn preview-workflow
  "This service will convert a JSON document in the format consumed by
   the import service into the format required by the DE."
  [body]
  (.previewWorkflow (workflow-preview-service) (slurp body)))

(defn import-template
  "This service will import a template into the DE."
  [body]
  (.importTemplate (workflow-import-service) (slurp body))
  (empty-response))

(defn import-workflow
  "This service will import a workflow into the DE."
  [body]
  (.importWorkflow (workflow-import-service) (slurp body)))

(defn import-tools
  "This service will import deployed components into the DE and send
   notifications if notification information is included and the deployed
   components are successfully imported."
  [body]
  (.updateWorkflow (workflow-import-service) (slurp body))
  (success-response))

(defn update-app
  "This service will update the information at the top level of an analysis.
   It will not update any of the components of the analysis."
  [body]
  (.updateAnalysisOnly (workflow-import-service) (slurp body))
  (success-response))

(defn update-template
  "This service will either update an existing template or import a new template."
  [body]
  (.updateTemplate (workflow-import-service) (slurp body))
  (empty-response))

(defn update-template-secured
  "This service will either update an existing template or import a new template.  The template
   ID is returned in the response body."
  [body]
  (.updateTemplate (workflow-import-service) (slurp body)))

(defn- add-user-info
  [m]
  (if current-user
    (let [full_name  (str (.getFirstName current-user) " " (.getLastName current-user))
          email      (.getEmail current-user)
          username   (.getUsername current-user)]
      (assoc m
        :implementation {:implementor       full_name
                         :implementor_email email
                         :test              {:params []}}
        :full_username  username))
    m))

(defn update-app-secured
  "This service will either update an existing single-step app or import a new one. The app ID
   is returned in the response body."
  [body]
  (.updateTemplate
   (workflow-import-service)
   (-> (parse-json body)
       (app-meta-tx/template-external-to-internal)
       (add-user-info)
       (cheshire/encode))))

(defn update-workflow-from-json
  "This service will either update an existing workflow or import a new workflow
   from the given JSON string."
  [json]
  (.updateWorkflow
   (workflow-import-service)
   (-> (parse-json json)
       (update-in [:analyses] #(map add-user-info %))
       (cheshire/encode))))

(defn update-workflow
  "This service will either update an existing workflow or import a new workflow."
  [body]
  (update-workflow-from-json (slurp body)))

(defn force-update-workflow
  "This service will either update an existing workflow or import a new workflow.
   Vetted workflows may be updated."
  [body {:keys [update-mode]}]
  (.forceUpdateWorkflow (workflow-import-service) (slurp body) update-mode))

(defn- validate-param
  [param-value param-name]
  (when (string/blank? param-value)
    (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
             :reason     :MISSING_OR_EMPTY_QUERY_STRING_PARAMETER
             :param_name param-name})))

(defn- string-to-long
  [param-value param-name]
  (try+
   (Long/parseLong param-value)
   (catch NumberFormatException e
     (throw+ {:error_code ce/ERR_ILLEGAL_ARGUMENT
              :reason     :LONG_INTEGER_EXPECTED
              :param_name param-name}))))

(defn bootstrap
  "This service obtains information about and initializes the workspace for
   the authenticated user. It also records the user's login so that we can
   obtain statistics later."
  [ip-address user-agent]
  (validate-param ip-address :ip-address)
  (validate-param user-agent :user-agent)
  (let [username   (.getUsername current-user)
        user-info  (.getCurrentUserInfo (user-service))
        login-time (record-login (.getUsername current-user) ip-address user-agent)]
    {:workspaceId  (.getWorkspaceId user-info)
     :newWorkspace (.isNewWorkspace user-info)
     :loginTime    (str login-time)
     :username     (.getShortUsername current-user)
     :full_username username
     :email        (.getEmail current-user)
     :firstName    (.getFirstName current-user)
     :lastName     (.getLastName current-user)}))

(defn logout
  "This service records explicit logouts for users so that we can obtain
   statistics later."
  [{:keys [ip-address login-time]}]
  (validate-param ip-address :ip-address)
  (validate-param login-time :login-time)
  (record-logout (.getUsername current-user) ip-address (string-to-long login-time :login-time))
  {})

(defn run-experiment
  "This service accepts a job submission from a user then reformats it and
   submits it to the JEX."
  [body workspace-id]
  (->> (add-workspace-id (slurp body) workspace-id)
       object->json-obj
       (.runExperiment (experiment-runner))
       (#(cheshire/decode % true))
       :job_id
       vector
       (get-selected-analyses (string->long workspace-id))
       first
       success-response))

(defn rate-app
  "This service adds a user's rating to an app."
  [body]
  (.rateAnalysis (rating-service) (slurp body)))

(defn delete-rating
  "This service removes a user's rating from an app."
  [body]
  (.deleteRating (rating-service) (slurp body)))

(defn list-deployed-components-in-app
  "This service lists all of the deployed components in an app."
  [app-id]
  (.listDeployedComponentsInAnalysis (analysis-listing-service) app-id))

(defn list-app
  "This service lists a single application.  The response body contains a JSON
   string representing an object containing a list of apps.  If an app with the
   provided identifier exists then the list will contain that app.  Otherwise,
   the list will be empty."
  [app-id]
  (.listAnalysis (analysis-listing-service) app-id))

(defn update-favorites
  "This service adds apps to or removes apps from a user's favorites list."
  [body]
  (let [workspace (ws/get-or-create-workspace (.getUsername current-user))
        request   (assoc (parse-json body) :workspace_id (:id workspace))]
    (.updateFavorite (analysis-categorization-service) (cheshire/encode request))))

(defn edit-app
  "This service makes an app available in Tito for editing."
  [app-id]
  (.prepareAnalysisForEditing (analysis-edit-service) app-id))

(defn edit-app-new-format
  "This service makes an app available for editing in Tito and returns a
   representation of it in the JSON format required by the DE as of version
   1.8."
  [app-id]
  (-> (edit-app app-id)
      (cheshire/decode true)
      (:objects)
      (first)
      (app-meta-tx/template-internal-to-external)
      (cheshire/encode)))

(defn copy-app
  "This service makes a copy of an app available in Tito for editing."
  [app-id]
  (.copyAnalysis (analysis-edit-service) app-id))

(defn make-app-public
  "This service copies an app from a user's private workspace to the public
   workspace."
  [body]
  (let [body                        (slurp body)
        app-id                      (:analysis_id (parse-json body))
        [publishable? msg bad-apps] (app-publishable? app-id)]
    (if publishable?
      (.makeAnalysisPublic (template-group-service) body)
      (do (log/debug msg)
          {:status 400
           :body   (cheshire/encode {:success false
                                     :reason  msg
                                     :apps    (map :name bad-apps)})}))))

(defn get-property-values
  "Gets the property values for a previously submitted job."
  [job-id]
  (-> (.getPropertyValues (property-value-service) job-id)
      (cheshire/decode true)
      (prop-value-tx/format-property-values-response)
      (cheshire/encode)))

(defn- get-unformatted-app-rerun-info
  "Obtains an analysis representation with the property values from a previous experiment
   plugged into the appropriate properties. The analysis representation is left in a Clojure
   data structure so that further processing can be done prior to serialization."
  [job-id]
  (let [values        (cheshire/decode (.getPropertyValues (property-value-service) job-id) true)
        app           (cheshire/decode (get-app (:analysis_id values)) true)
        pval-to-entry #(vector (:full_param_id %) (:param_value %))
        values        (into {} (map pval-to-entry (:parameters values)))
        update-prop   #(let [id (:id %)]
                         (if (contains? values id)
                           (assoc % :value (values id))
                           %))
        update-props  #(map update-prop %)
        update-group  #(update-in % [:properties] update-props)
        update-groups #(map update-group %)]
    (update-in app [:groups] update-groups)))

(defn get-app-rerun-info
  "Obtains analysis JSON with the property values from a previous experiment
   plugged into the appropriate properties."
  [job-id]
  (success-response (get-unformatted-app-rerun-info job-id)))

(defn get-new-app-rerun-info
  "Obtains analysis JSON in the new format required by the DE with the property values from a
   previous experiment plugged into the appropriate properties."
  [job-id]
  (-> (get-unformatted-app-rerun-info job-id)
      (app-meta-tx/template-internal-to-external)
      (success-response)))

(defn list-reference-genomes
  "Lists the reference genomes in the database."
  []
  (success-response {:genomes (get-reference-genomes)}))

(defn replace-reference-genomes
  "Replaces teh reference genomes in the database with a new set of reference
   genomes."
  [body]
  (put-reference-genomes (:genomes (cheshire/decode body true)))
  (success-response))
