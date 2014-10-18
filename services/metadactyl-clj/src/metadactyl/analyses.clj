(ns metadactyl.analyses
  (:use [kameleon.entities]
        [kameleon.uuids :only [uuid]]
        [korma.core]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [try+]])
  (:require [clojure.tools.logging :as log]
            [metadactyl.metadata.params :as mp]
            [metadactyl.persistence.app-metadata :as ap]))

(def ^:private input-mulitplicities
  {"FileInput"         "single"
   "FolderInput"       "collection"
   "MultiFileSelector" "many"})

(def ^:private input-types
  (set (keys input-mulitplicities)))

(def ^:private output-multiplicities
  {"FileOutput"      "single"
   "FolderOutput"    "collection"
   "MultiFileOutput" "many"})

(def ^:private output-types
  (set (keys output-multiplicities)))

(defn input?
  [{:keys [type]}]
  (input-types type))

(defn output?
  [{:keys [type]}]
  (output-types type))

(defn- load-steps
  [app-id]
  (select app_steps (where {:app_id app-id})))

(defn- qual-id
  [step-id param-id]
  (str step-id "_" param-id))

(defn- param->qual-id
  [param]
  (qual-id (:step_id param) (:id param)))

(defn- format-io-map
  [mapping]
  [(qual-id (:source_step mapping) (:output mapping))
   (qual-id (:target_step mapping) (:input mapping))])

(defn- load-io-maps
  [app-id]
  (->> (select [:workflow_io_maps :wim]
               (join [:input_output_mapping :iom] {:wim.id :iom.mapping_id})
               (fields :wim.source_step :iom.output :wim.target_step :iom.input)
               (where {:wim.app_id app-id}))
       (map format-io-map)))

(defn- build-component
  [{task-id :task_id}]
  (assert-not-nil
   [:tool-for-task task-id]
   (->> (select :tasks
                (join :tools {:tasks.tool_id :tools.id})
                (join :tool_types {:tools.tool_type_id :tool_types.id})
                (fields :tools.description :tools.location :tools.name [:tool_types.name :type])
                (where {:tasks.id task-id}))
        (first))))

;; TODO: format the input in the way that the JEX requires. See metadactyl's json.log for
;; more details. This should be pretty easy, but it's going to be necessary to grab some
;; additional information from the database.
(defn- build-input
  [config step-id param]
  (assoc param :value (config (keyword (param->qual-id param)))))

;; TODO: the current implementation performs the lookup in the config twice for each input,
;; which isn't terrible, but could be a little more efficient. On the other hand, I think
;; that the current implementation is clear this way, because the filtering is explicit.
;; Decide whether to keep this implementation or make it more efficient.
(defn- build-inputs
  [config step-id params]
  (->> (filter input? params)
       (filter (comp config keyword param->qual-id))
       (map (partial build-input config step-id))))

;; TODO: implement me. All outputs are included in the list, even intermediate outputs. We
;; also need to include an output definition for the logs directory, which will not appear
;; in the list of parameters.
(defn- build-outputs
  [config io-maps default-values step-id params]
  {})

;; TODO: implement me. The values in this case are the values that go on the command line.
;; This is the one case in which we're going to have to consider the parameter type before
;; formatting the job submission JSON.
;;
;; For inputs, the value is always either mapped or provided in the config. Mapped inputs
;; require some special attention because the output value may be in the config or in the
;; parmeter's default value. I don't think we have to worry about a multi-file output being
;; mapped to a multi-file input yet; it doesn't look like the DE supports that yet.
;;
;; For outputs and parameters, the default parameter value is used if a parameter value isn't
;; specified in the config.
(defn- build-params
  [config io-maps default-values step-id params]
  {})

(defn- build-config
  [config io-maps default-values step-id params]
  {:input  (build-inputs config step-id params)
   :output (build-outputs config io-maps default-values step-id params)
   :params (build-params config io-maps default-values step-id params)})

;; TODO: implement me. The environment map comes from the parameter list. Any parameter of type,
;; EnvironmentVariable goes in this map instead of the command-line configuration.
(defn- build-environment-map
  [submission step]
  {})

(defn- build-step
  [config io-maps default-values params {step-id :id :as step}]
  (let [step-params (params step-id)]
    {:component   (build-component step)
     :config      (build-config config io-maps default-values step-id step-params)
     :environment (build-environment-map config step)
     :type        "condor"}))

(defn- build-default-values-map
  [params]
  (into {} (map (juxt param->qual-id :default_value) params)))

(defn- build-steps
  [app {:keys [config] :as submission}]
  (let [steps-to-skip  (dec (:starting_step submission 1))
        io-maps        (load-io-maps (:id app))
        params         (mp/load-app-params (:id app))
        default-values (build-default-values-map params)
        params         (group-by :step_id params)]
    (->> (load-steps (:id app))
         (drop steps-to-skip)
         (take-while (comp nil? :external_app_id))
         (mapv (partial build-step config io-maps default-values params)))))

(defn- build-submission
  [user email submission app]
  {:analysis_description (:description app)
   :analysis_id          (:id app)
   :analysis_name        (:name app)
   :callback             (:callback submission)
   :create_output_subdir (:create_output_subdir submission true)
   :description          (:description submission "")
   :email                email
   :execution_target     "condor"
   :name                 (:name submission)
   :notify               (:notify submission)
   :output_dir           (:output_dir submission)
   :request_type         "submit"
   :steps                (build-steps app submission)
   :username             user
   :uuid                 (or (:uuid submission) (uuid))})

(defn submit
  [{:keys [user email]} submission]
  (->> (ap/get-app (:app_id submission))
       (build-submission user email submission)
       (remove-nil-vals)))
