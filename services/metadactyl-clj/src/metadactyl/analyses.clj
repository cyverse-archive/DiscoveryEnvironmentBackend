(ns metadactyl.analyses
  (:use [kameleon.entities]
        [kameleon.uuids :only [uuid uuidify]]
        [korma.core]
        [metadactyl.user :only [current-user]]
        [metadactyl.util.assertions :only [assert-not-nil]]
        [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [try+ throw+]])
  (:require [cheshire.core :as cheshire]
            [clj-http.client :as http]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [me.raynes.fs :as fs]
            [metadactyl.metadata.params :as mp]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.util.config :as config]))


(def not-blank? (complement string/blank?))

(def ^:private input-multiplicities
  {"FileInput"         "single"
   "FolderInput"       "collection"
   "MultiFileSelector" "many"})

(def ^:private input-types
  (set (keys input-multiplicities)))

(def ^:private output-multiplicities
  {"FileOutput"      "single"
   "FolderOutput"    "collection"
   "MultiFileOutput" "many"})

(def ^:private output-types
  (set (keys output-multiplicities)))

(def ^:private log-output
  {:multiplicity "collection"
   :name         "logs"
   :property     "logs"
   :type         "File"
   :retain       true})

(def ^:private environment-variable-type
  "EnvironmentVariable")

(def ^:private ignored-param-types
  #{environment-variable-type})

(defn input?
  [{:keys [type]}]
  (input-types type))

(defn output?
  [{:keys [type]}]
  (output-types type))

(defn executable?
  [component-type]
  (= component-type "executable"))

(def ^:private generated-param-ids
  {:archive      (uuidify "06F127BB-0599-4343-89CB-DD15BD0163DE")
   :archive-path (uuidify "21039F17-3D4B-4E33-BD10-2904408826F8")
   :command      (uuidify "3046E5B2-F617-49BE-9C26-57D257F2331B")
   :job-name     (uuidify "0D1DB50C-C930-4CDA-8FEC-2E7326B26849")
   :proxy-user   (uuidify "616D8815-C42A-4E53-885B-E7A594D4BDBD")})

(def ^:private irods-home-pattern
  (memoize #(re-pattern (str "\\A\\Q" (string/replace (config/irods-home) #"/+\z") ""))))

(defn- remove-irods-home
  [path]
  (string/replace path (irods-home-pattern) ""))

(defn qual-id
  [step-id param-id]
  (str step-id "_" param-id))

(defn param->qual-id
  [param]
  (qual-id (:step_id param) (:id param)))

(def param->qual-key (comp keyword param->qual-id))

(defn load-steps
  [app-id]
  (select app_steps (where {:app_id app-id})))

(defn- format-io-map
  [mapping]
  [(qual-id (:target_step mapping) (:input mapping))
   (qual-id (:source_step mapping) (:output mapping))])

(defn load-io-maps
  [app-id]
  (->> (select [:workflow_io_maps :wim]
               (join [:input_output_mapping :iom] {:wim.id :iom.mapping_id})
               (fields :wim.source_step :iom.output :wim.target_step :iom.input)
               (where {:wim.app_id app-id}))
       (map format-io-map)
       (into {})))

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

(defn- build-input
  "Builds a single input for a step in an app. The current implementation performs the analysis
  configuration lookup twice (once in build-input and once in build-inputs), but the code seems
  clearest that way."
  [config param]
  (let [path     (config (param->qual-key param))
        filename (when-not (nil? path) (fs/base-name path))]
    {:id           (:id param)
     :multiplicity (input-multiplicities (:type param))
     :name         filename
     :property     filename
     :retain       (:retain param)
     :type         (:type param)
     :value        path}))

(defn- build-inputs
  "Builds the list of inputs for a step in an app. The current implementation performs the
  analysis configuration lookup twice, but the code seems clearest that way."
  [config params]
  (->> (filter input? params)
       (filter (comp config param->qual-key))
       (map (partial build-input config))))

(defn- missing-output-filename
  [{step-id :step_id id :id}]
  (throw+ {:error_code ce/ERR_BAD_REQUEST
           :reason     ("no filename found for output " id " in step " step-id)}))

(defn- get-output-filename
  "Obtains the name of an output filename from either the app config or the default values
  for the job submission."
  [config default-values param]
  (let [qual-id  (param->qual-id param)
        qual-key (keyword qual-id)]
    (cond (contains? config qual-key)        (config qual-key)
          (contains? default-values qual-id) (default-values qual-id)
          :else                              (missing-output-filename param))))

(defn- build-output
  [config default-values param]
  (let [filename (get-output-filename config default-values param)]
    {:multiplicity (output-multiplicities (:type param))
     :name         filename
     :property     filename
     :qual-id      (param->qual-id param)
     :retain       (:retain param)
     :type         (:info_type param)}))

(defn- build-outputs
  [config default-values params]
  (conj (mapv (partial build-output config default-values)
              (filter output? params))
        log-output))

(defn- value-for-param
  ([config io-maps output-value-map default-values param]
     (let [qual-id  (param->qual-id param)
           qual-key (keyword qual-id)]
       (cond (contains? io-maps qual-id)        (output-value-map qual-id)
             (contains? config qual-key)        (config qual-key)
             (contains? default-values qual-id) (default-values qual-id)
             :else                              nil)))
  ([config default-values param]
     (value-for-param config {} {} default-values param)))

(defn- build-arg
  ([param param-name param-value]
     {:id    (:id param)
      :name  param-name
      :order (:order param 0)
      :value (if (nil? param-value) "" param-value)})
  ([param param-value]
     (build-arg param (or (:name param) "") param-value)))

(defn- generic-args
  [param param-value]
  (if (or (not-blank? param-value) (not (:omit_if_blank param)))
    [(build-arg param param-value)]
    []))

(defn- is-selection-arg?
  [param-value]
  (and (map? param-value)
       (or (seq (:name param-value))
           (seq (:value param-value)))))

(defn- selection-args
  [param param-value]
  (if (is-selection-arg? param-value)
    [(build-arg param (or (:name param-value) "") (or (:value param-value) ""))]
    []))

(defn- tree-selection-args
  [param param-value]
  (let [selections (if (seq? param-value) param-value [])]
    (mapcat (partial selection-args param) selections)))

(defn- flag-args
  [param param-value]
  (let [selected?      (Boolean/parseBoolean (string/trim param-value))
        values         (string/split (:name param) #"\s*,\s*" 2)
        selected-value (if selected? (first values) (second values))]
    (if (not-blank? selected-value)
      [(build-arg param selected-value)]
      [])))

(defn- input-args
  [param param-value]
  (let [values (if (seq? param-value) param-value [param-value])]
    (mapv (comp (partial build-arg param) (fnil fs/base-name ""))
          (if (:omit_if_blank param) (remove string/blank? values) values))))

(defn- output-args
  [param param-value]
  (if (and (not (:is_implicit param)) (= (:data_source param) "file"))
    (generic-args param param-value)
    []))

(defn- build-reference-genome-args
  [filenames param param-value]
  (if-let [path (:path param-value)]
    [(->> (map #(str (fs/file path %)) filenames)
          (string/join " ")
          (build-arg param))]
    []))

(def ^:private reference-genome-args
  ;; FIXME: this is functionally a reimplementation of the code that resolves reference
  ;; genomes in the old metadactyl code, which is probably broken. When time permits,
  ;; look for uses of the 'ReferenceGenome' property type to see if and how it's being
  ;; used and whether or not this implementation of the command-line formatting code will
  ;; work.
  (partial build-reference-genome-args ["annotation.gtf" "genome.fas"]))

(def ^:private reference-sequence-args
  (partial build-reference-genome-args ["genome.fas"]))

(def ^:private reference-annotation-args
  (partial build-reference-genome-args ["annotation.gtf"]))

(defn- args-for-param
  [config io-maps output-value-map default-values param]
  (let [param-value (value-for-param config io-maps output-value-map default-values param)
        param-type  (:type param)]
    (cond
     (= "TreeSelection" param-type)
     (tree-selection-args param param-value)

     (re-find #"Selection\z" param-type)
     (selection-args param param-value)

     (= "Flag" param-type)
     (flag-args param param-value)

     (input-types param-type)
     (input-args param param-value)

     (output-types param-type)
     (output-args param param-value)

     (= "ReferenceGenome" param-type)
     (reference-genome-args param param-value)

     (= "ReferenceSequence" param-type)
     (reference-sequence-args param param-value)

     (= "ReferenceAnnotation" param-type)
     (reference-annotation-args param param-value)

     :else
     (generic-args param param-value))))

(defn- build-params
  [config io-maps outputs default-values params]
  (let [output-value-map (into {} (map (juxt :qual-id :name) outputs))]
    (mapcat (partial args-for-param config io-maps output-value-map default-values)
            (remove (comp ignored-param-types :type) params))))

(defn- build-config
  [build-extra-params config io-maps default-values params]
  (let [outputs (build-outputs config default-values params)]
    {:input  (build-inputs config params)
     :output (map (fn [m] (dissoc m :qual-id)) outputs)
     :params (concat (build-extra-params)
                     (build-params config io-maps outputs default-values params))}))

(defn- generate-extra-arg
  [order name value id-key]
  {:id    (generated-param-ids id-key)
   :name  name
   :order order
   :value value})

(defn- build-extra-fapi-args
  [job-name output-dir]
  [(generate-extra-arg 0 "run" "" :command)
   (generate-extra-arg 1 "--proxy_user=" (:shortUsername current-user) :proxy-user)
   (generate-extra-arg 1 "--jobName=" job-name :job-name)
   (generate-extra-arg 1 "--archive" "" :archive)
   (generate-extra-arg 1 "--archivePath=" (remove-irods-home output-dir) :archive-path)])

(defn- get-config-builder
  [component-type job-name output-dir]
  (if (executable? component-type)
    (partial build-config (constantly []))
    (partial build-config (partial build-extra-fapi-args job-name output-dir))))

(defn- build-environment-entries
  [config default-values param]
  (let [value (value-for-param config default-values param)]
    (if (or (not-blank? value) (not (:omit_if_blank param)))
      [[(:name param) value]]
      [])))

(defn- build-environment-map
  [config default-values params]
  (->> (filter #(= (:type %) environment-variable-type) params)
       (mapcat (partial build-environment-entries config default-values))
       (into {})))

(defn- get-env-builder
  [component-type]
  (if (executable? component-type)
    build-environment-map
    (constantly {})))

(defn- build-step
  [{config :config :as submission} io-maps default-values params {step-id :id :as step}]
  (let [step-params    (params step-id)
        component      (build-component step)
        env-builder    (get-env-builder (:type component))
        component-type (:type component)
        job-name       (:name submission)
        output-dir     (:output_dir submission)
        config-builder (get-config-builder component-type job-name output-dir)]
    {:component   (build-component step)
     :config      (config-builder config io-maps default-values step-params)
     :environment (env-builder config default-values step-params)
     :type        "condor"}))

(defn- build-default-values-map
  [params]
  (into {} (map (juxt param->qual-id :default_value) params)))

(defn- build-steps
  [app submission]
  (let [steps-to-skip  (dec (:starting_step submission 1))
        io-maps        (load-io-maps (:id app))
        params         (mp/load-app-params (:id app))
        default-values (build-default-values-map params)
        params         (group-by :step_id params)]
    (->> (load-steps (:id app))
         (drop steps-to-skip)
         (take-while (comp nil? :external_app_id))
         (mapv (partial build-step submission io-maps default-values params)))))

(defn- build-submission
  [user email submission app]
  {:analysis_details     (:description app)
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
   :uuid                 (or (:uuid submission) (uuid))
   :wiki_url             (:wikiurl app)})

(defn submit-job
  [submission]
  (try+
   (http/post (config/jex-base-url)
              {:body         (cheshire/encode submission)
               :content-type :json})
   (catch Object o
     (log/error (:throwable &throw-context) "job submission failed")
     (throw+ {:error_code ce/ERR_REQUEST_FAILED})))
  submission)

(defn submit
  [{:keys [user email]} submission]
  (->> (ap/get-app (:app_id submission))
       (build-submission user email submission)
       (remove-nil-vals)
       (submit-job)))
