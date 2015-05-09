(ns metadactyl.service.apps.jobs.params
  (:use [metadactyl.util.conversions :only [remove-nil-vals]]
        [slingshot.slingshot :only [throw+]])
  (:require [cheshire.core :as cheshire]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [metadactyl.persistence.app-metadata :as ap]
            [metadactyl.service.util :as util]))

(defn- get-job-submission-config
  [job]
  (let [submission (:submission job)]
    (when-not submission
      (throw+ {:error_code ce/ERR_NOT_FOUND
               :reason     "Job submission values could not be found."}))
    (:config (cheshire/decode (.getValue submission) true))))

(defn- load-mapped-params
  [app-id]
  (if (util/uuid? app-id)
    (let [format-id     (partial string/join "_")
          get-source-id (comp format-id (juxt :source_id (some-fn :output_id :external_output_id)))
          get-target-id (comp format-id (juxt :target_id (some-fn :input_id :external_input_id)))
          get-ids       (juxt get-source-id get-target-id)]
      (set (mapcat get-ids (ap/load-app-mappings app-id))))
    #{}))

(defn- get-full-param-id
  [param]
  (str (:step_id param) "_" (:id param)))

(defn- remove-mapped-params
  [app-id params]
  (let [mapped-params (load-mapped-params app-id)]
    (remove (comp mapped-params get-full-param-id) params)))

(def property-types-to-omit #{"Info"})

(defn- implicit-output?
  [param]
  (and (= "Output" (:value_type param)) (:is_implicit param)))

(defn- omit-param?
  [param]
  (or (contains? property-types-to-omit (:type param))
      (implicit-output? param)))

(defn- blank->empty-str
  [value]
  (if (string/blank? value)
    ""
    value))

(defn- format-list-selection
  [value]
  (assoc value
    :display (:display value (:name value ""))
    :value   (:value value (:name value ""))))

(defn- format-scalar-value
  [value]
  {:value (blank->empty-str (str value))})

(defn- format-config-values
  [config-values]
  (cond (sequential? config-values) (map format-scalar-value config-values)
        (map? config-values)        [(format-list-selection config-values)]
        :else                       [(format-scalar-value config-values)]))

(defn- format-job-param-values
  [config config-key default-value]
  (if-let [config-values (config-key config)]
    (format-config-values config-values)
    [default-value]))

(defn- format-job-param-for-value
  [full-param-id default-value param value]
  (remove-nil-vals
   {:param_type       (:type param)
    :data_format      (blank->empty-str (:data_format param))
    :info_type        (blank->empty-str (:info_type param))
    :is_visible       (:is_visible param true)
    :param_id         (:id param)
    :full_param_id    full-param-id
    :param_value      value
    :is_default_value (= value default-value)
    :param_name       (:label param)}))

(defn- format-job-param
  [config param]
  (let [full-param-id (get-full-param-id param)
        config-key    (keyword full-param-id)
        default-value (first (format-config-values (:default_value param)))
        values        (format-job-param-values config config-key default-value)]
    (map (partial format-job-param-for-value full-param-id default-value param) values)))

(defn get-parameter-values
  [apps-client {:keys [app-id] :as job}]
  (let [config (get-job-submission-config job)]
    (->> (.getParamDefinitions apps-client app-id)
         (remove-mapped-params app-id)
         (remove omit-param?)
         (mapcat (partial format-job-param config)))))
