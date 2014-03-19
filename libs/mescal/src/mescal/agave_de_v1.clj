(ns mescal.agave-de-v1
  (:require [clojure.string :as string]))

(def ^:private hpc-group-description "Apps that run on HPC resources.")
(def ^:private hpc-group-name "High-Performance Computing")
(def ^:private hpc-group-id "HPC")
(def ^:private unknown-value "UNKNOWN")

(def ^:private hpc-group-overview
  {:id   hpc-group-id
   :name hpc-group-name})

(defn- regex-quote
  [s]
  (java.util.regex.Pattern/quote s))

(def ^:private de-to-agave-path-regex
  (memoize (fn [irods-home] (re-pattern (str "^" (regex-quote irods-home) "|/$")))))

(defn- de-to-agave-path
  [path irods-home]
  (when-not (string/blank? path)
    (string/replace path (de-to-agave-path-regex irods-home) "")))

(defn- agave-to-de-path
  [path irods-home]
  (when-not (string/blank? path)
    (str (string/replace irods-home #"/$" "")
         "/"
         (string/replace path #"^/" ""))))

(defn public-app-group
  []
  {:description    hpc-group-description
   :id             hpc-group-id
   :is_public      true
   :name           hpc-group-name
   :template_count -1
   :workspace_id   0})

(defn- system-statuses
  [agave]
  (into {} (map (fn [m] [(:resource.id m) (:status m)]) (.listSystems agave))))

(defn- app-enabled?
  [statuses jobs-enabled? listing]
  (and jobs-enabled?
       (:available listing)
       (= "up" (statuses (:executionHost listing)))))

(defn- get-app-name
  [app]
  (let [app-name (first (remove string/blank? (map app [:label :name :id])))
        app-id   (:id app)]
    (str app-name " [" app-id "]")))

(defn- format-app-listing
  [statuses jobs-enabled? listing]
  (let [disabled? (not (app-enabled? statuses jobs-enabled? listing))
        app-name  (get-app-name listing)]
    (-> listing
        (dissoc :available :checkpointable :deploymentPath :executionHost :executionType
                :helpURI :inputs :longDescription :modules :ontolog :outputs :parallelism
                :parameters :public :revision :shortDescription :tags :templatePath
                :testPath :version)
        (assoc
            :can_run              true
            :can_favor            false
            :can_rate             false
            :deleted              false
            :description          (:shortDescription listing)
            :disabled             disabled?
            :edited_date          (System/currentTimeMillis)
            :group_id             hpc-group-id
            :group_name           hpc-group-name
            :integration_date     (System/currentTimeMillis)
            :integrator_email     unknown-value
            :integrator_name      unknown-value
            :is_favorite          false
            :is_public            (:public listing)
            :pipeline_eligibility {:is_valid false :reason "HPC App"}
            :rating               {:average 0.0}
            :step-count           1
            :wiki_url             ""
            :name                 app-name))))

(defn- comparator-for
  [sort-dir]
  (let [lc (fnil string/lower-case "")]
    (if (= "desc" (string/lower-case (or sort-dir "asc")))
      #(compare (lc %2) (lc %1))
      #(compare (lc %1) (lc %2)))))

(defn- sort-results
  [results sort-field sort-dir]
  (if sort-field
    (sort-by (keyword sort-field) (comparator-for sort-dir) results)
    results))

(defn- parse-long
  [s]
  (when-not (string/blank? s)
    (try
      (Long/parseLong s)
      (catch NumberFormatException ignore nil))))

(defn- apply-offset
  [results offset-str]
  (if-let [offset (parse-long offset-str)]
    (drop offset results)
    results))

(defn- apply-limit
  [results limit-str]
  (if-let [limit (parse-long limit-str)]
    (take limit results)
    results))

(defn- apply-params
  [results params]
  (-> results
      (sort-results (:sortField params) (:sortDir params))
      (apply-offset (:offset params))
      (apply-limit (:limit params))))

(defn list-public-apps
  [agave jobs-enabled? params]
  (let [statuses (system-statuses agave)
        listing  (.listPublicApps agave)
        total    (count listing)
        listing  (map (partial format-app-listing statuses jobs-enabled?) listing)
        listing  (apply-params listing params)]
    (assoc (public-app-group)
      :templates      listing
      :template_count total)))

(defn- app-matches?
  [search-term app]
  (some (fn [s] (re-find (re-pattern (str "(?i)\\Q" search-term)) s))
        ((juxt :name :description) app)))

(defn- find-matching-apps
  [agave jobs-enabled? search-term]
  (filter (partial app-matches? search-term)
          (:templates (list-public-apps agave jobs-enabled?))))

(defn search-public-apps
  [agave jobs-enabled? search-term]
  (let [matching-apps (find-matching-apps agave jobs-enabled? search-term)]
    {:template_count (count matching-apps)
     :templates      matching-apps}))

(defn format-deployed-component-for-app
  [{path :deploymentPath :as app}]
  {:attribution ""
   :description (:shortDescription app)
   :id          (:id app)
   :location    (string/replace path #"/[^/]+$" "")
   :name        (string/replace path #"^.*/" "")
   :type        (:executionType app)
   :version     (:version app)})

(defn get-deployed-component-for-app
  [agave app-id]
  (format-deployed-component-for-app (.getApp agave app-id)))

(defn get-app-details
  [agave app-id]
  (let [now      (str (System/currentTimeMillis))
        app      (.getApp agave app-id)
        app-name (get-app-name app)]
    {:published_date   now
     :edited_date      now
     :id               app-id
     :refrences        []
     :description      (:shortDescription app)
     :name             app-name
     :label            app-name
     :tito             app-id
     :components       [(format-deployed-component-for-app app)]
     :groups           [hpc-group-overview]
     :suggested_groups [hpc-group-overview]}))

(defn- get-boolean
  [value default]
  (cond (nil? value)    default
        (string? value) (Boolean/parseBoolean value)
        :else           value))

(defn- format-group
  [name params]
  (when (some :isVisible params)
    {:name       name
     :label      name
     :id         name
     :type       ""
     :properties params}))

(defn- format-input-validator
  [input]
  {:required (get-boolean (get-in input [:value :required]) false)})

(defn- number-type-for
  [xsd-type]
  (cond
   (= xsd-type "xs:decimal")       "Double"
   (= xsd-type "xs:float")         "Double"
   (= xsd-type "xs:double")        "Double"
   (= xsd-type "xs:integer")       "Integer"
   (= xsd-type "xs:long")          "Integer"
   (= xsd-type "xs:int")           "Integer"
   (= xsd-type "xs:short")         "Integer"
   (= xsd-type "xs:byte")          "Integer"
   (= xsd-type "xs:unsignedLong")  "Integer"
   (= xsd-type "xs:unsignedInt")   "Integer"
   (= xsd-type "xs:unsignedShort") "Integer"
   (= xsd-type "xs:unsignedByte")  "Integer"
   :else                           "Double"))

(defn- string-type-for
  [xsd-type]
  (cond
   (= xsd-type "xs:boolean") "Flag"
   :else                     "Text"))

(defn- get-param-type
  [param]
  (let [type     (get-in param [:value :type])
        ontology (get-in param [:semantics :ontology])
        xsd-type (first (filter (partial re-matches #"xs:.*") ontology))
        regex    (get-in param [:value :validator]) ]
    (cond
     (= type "number") (number-type-for xsd-type)
     (= type "string") (string-type-for xsd-type)
     (= type "bool")   "Flag")))

(defn- format-param
  [get-type get-value param]
  {:arguments    []
   :defaultValue (get-value param)
   :description  (get-in param [:details :description])
   :id           (:id param)
   :isVisible    (get-boolean (get-in param [:value :visible]) false)
   :label        (get-in param [:details :label])
   :name         (:id param)
   :order        0
   :required     (get-boolean (get-in param [:value :required]) false)
   :type         (get-type param)
   :validators   []})

(defn- param-formatter
  [get-type get-value]
  (fn [param]
    (format-param get-type get-value param)))

(defn- get-default-param-value
  [param]
  (get-in param [:value :default]))

(defn- input-param-formatter
  [irods-home & {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter (constantly "FileInput") #(agave-to-de-path (get-default %) irods-home)))

(defn- opt-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter get-param-type get-default))

(defn- output-param-formatter
  [& {:keys [get-default] :or {get-default get-default-param-value}}]
  (param-formatter (constantly "Output") get-default))

(defn- format-groups
  [irods-home app]
  (remove nil?
          [(format-group "Inputs" (map (input-param-formatter irods-home) (:inputs app)))
           (format-group "Parameters" (map (opt-param-formatter) (:parameters app)))
           (format-group "Outputs" (map (output-param-formatter) (:outputs app)))]))

(defn- format-app
  [app group-formatter]
  (let [app-label (get-app-name app)]
    {:id           (:id app)
     :name         app-label
     :label        app-label
     :component_id hpc-group-id
     :groups       (group-formatter app)}))

(defn get-app
  [agave irods-home app-id]
  (format-app (.getApp agave app-id) (partial format-groups irods-home)))

(defn- get-archive-path
  [irods-home submission]
  (let [irods-home (string/replace irods-home #"/$" "")]
    (str (de-to-agave-path (:outputDirectory submission) irods-home)
         "/" (string/replace (:name submission) #"\s" "_"))))

(defn- fix-input-paths
  [irods-home app config]
  (reduce
   (fn [config {input-id :id}]
     (update-in config [(keyword input-id)] de-to-agave-path irods-home))
   config
   (:inputs app)))

(def ^:private default-processors "1")
(def ^:private default-memory "4")
(def ^:private default-runtime "1:00:00")

(defn- job-config
  [irods-home app submission]
  (assoc (fix-input-paths irods-home app (:config submission))
    :archive        true
    :archivePath    (get-archive-path irods-home submission)
    :jobName        (:name submission)
    :softwareName   (:id app)
    :processorCount (or (:defaultProcessors app) default-processors)
    :maxMemory      (or (:defaultMemory app) default-memory)
    :requestedTime  (or (:defaultRequestedTime app) default-runtime)))

(defn- app-info-for
  [app]
  {:name          (:label app "")
   :description   (:shortDescription app "")
   :available     (:available app)
   :executionHost (:executionHost app)})

(def ^:private submitted "Submitted")
(def ^:private running "Running")
(def ^:private failed "Failed")
(def ^:private completed "Completed")

(defn translate-job-status
  [status]
  (case status
    "PENDING"            submitted
    "STAGING_INPUTS"     submitted
    "CLEANING_UP"        running
    "ARCHIVING"          running
    "STAGING_JOB"        submitted
    "FINISHED"           running
    "KILLED"             failed
    "FAILED"             failed
    "STOPPED"            failed
    "RUNNING"            running
    "PAUSED"             running
    "QUEUED"             submitted
    "SUBMITTING"         submitted
    "STAGED"             submitted
    "PROCESSING_INPUTS"  submitted
    "ARCHIVING_FINISHED" completed
    "ARCHIVING_FAILED"   failed
                         "Running"))

(defn- build-path
  [base & rest]
  (string/join "/" (cons base (map #(string/replace % #"^/|/$" "") rest))))

(defn- format-job
  ([irods-home jobs-enabled? app-info-map job]
     (let [app-id   (:software job)
           app-info (app-info-map app-id {})]
       {:id               (str (:id job))
        :analysis_id      app-id
        :analysis_details (:description app-info "")
        :analysis_name    (get-app-name app-info)
        :description      ""
        :enddate          (str (:endTime job))
        :name             (:name job)
        :raw_status       (:status job)
        :resultfolderid   (build-path irods-home (:archivePath job))
        :startdate        (str (:submitTime job))
        :status           (translate-job-status (:status job))
        :wiki_url         ""}))
  ([irods-home jobs-enabled? statuses app-info-map job]
     (let [app-id   (:software job)
           app-info (app-info-map app-id {})]
       (assoc (format-job irods-home jobs-enabled? app-info-map job)
         :app-disabled (not (app-enabled? statuses jobs-enabled? app-info))))))

(defn submit-job
  [agave irods-home submission]
  (let [app-id (:analysis_id submission)
        app    (.getApp agave app-id)
        config (job-config irods-home app submission)]
    (format-job irods-home true (system-statuses agave) {app-id app}
                (.submitJob agave app config))))

(defn- load-app-info-for-jobs
  [agave jobs]
  (let [app-ids      (into #{} (map :software jobs))
        get-app-info #(app-info-for (.getApp agave %))]
    (into {} (map (juxt identity get-app-info) app-ids))))

(defn- format-jobs
  [agave irods-home jobs-enabled? jobs]
  (let [app-info (load-app-info-for-jobs agave jobs)
        statuses (system-statuses agave)]
    (map (partial format-job irods-home jobs-enabled? statuses app-info) jobs)))

(defn list-jobs
  ([agave jobs-enabled? irods-home]
     (format-jobs agave irods-home jobs-enabled? (.listJobs agave)))
  ([agave jobs-enabled? irods-home job-ids]
     (format-jobs agave irods-home jobs-enabled? (.listJobs agave job-ids))))

(defn list-raw-job
  [agave jobs-enabled? irods-home job-id]
  (let [job (.listJob agave job-id)]
    (format-job irods-home jobs-enabled? (load-app-info-for-jobs agave [job]) job)))

(defn list-job-ids
  [agave]
  (map (comp str :id) (.listJobs agave)))

(defn- format-param-value
  [get-val get-default get-type get-format get-info-type param]
  (let [default   (str (get-default))
        param-val (str (get-val))]
    {:data_format      (get-format)
     :full_param_id    (:id param)
     :info_type        (get-info-type)
     :is_default_value (= param-val default)
     :is_visible       (get-boolean (get-in param [:value :visible]) false)
     :param_id         (:id param)
     :param_name       (get-in param [:details :label] "")
     :param_type       (get-type param)
     :param_value      {:value param-val}}))

(defn- get-param-value
  [param-values param]
  (param-values (keyword (:id param)) ""))

(defn- get-default-param-value
  [param]
  (:defaultValue param ""))

(defn- format-input-param-value
  [irods-home param-values param]
  (format-param-value #(agave-to-de-path (get-param-value param-values param) irods-home)
                      #(agave-to-de-path (get-default-param-value param) irods-home)
                      (constantly "FileInput")
                      (constantly "Unspecified")
                      (constantly "File")
                      param))

(defn- format-opt-param-value
  [param-values param]
  (format-param-value #(get-param-value param-values param)
                      #(get-default-param-value param)
                      get-param-type
                      (constantly "")
                      (constantly "")
                      param))

(defn get-job-params
  [agave irods-home job-id]
  (let [job          (.listJob agave job-id)
        param-values (apply merge (apply concat ((juxt :inputs :parameters) job)))
        format-input (partial format-input-param-value irods-home param-values)
        format-opt   (partial format-opt-param-value param-values)
        app-id       (:software job)
        app          (.getApp agave app-id)]
    {:analysis_id app-id
     :parameters  (concat (mapv format-input (:inputs app))
                          (mapv format-opt (:parameters app)))}))

(defn- app-rerun-value-getter
  [job k]
  (let [values (apply merge (job k))]
    (fn [p]
      (or (values (keyword (:id p)))
          (get-default-param-value p)))))

(defn- format-groups-for-rerun
  [irods-home job app]
  (let [value-getter (partial app-rerun-value-getter job)
        format-input (input-param-formatter irods-home :get-default (value-getter :inputs))
        format-opt   (opt-param-formatter :get-default (value-getter :parameters))]
    (remove nil?
            [(format-group "Inputs" (map format-input (:inputs app)))
             (format-group "Parameters" (map format-opt (:parameters app)))
             (format-group "Outputs" (map (output-param-formatter) (:outputs app)))])))

(defn get-app-rerun-info
  [agave irods-home job-id]
  (let [job (.listJob agave job-id)
        app (.getApp agave (:software job))]
    (format-app app (partial format-groups-for-rerun irods-home job))))
