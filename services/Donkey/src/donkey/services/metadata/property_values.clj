(ns donkey.services.metadata.property-values
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.agave-apps :as agave-apps]
            [donkey.services.metadata.util :as mu]
            [donkey.util.service :as service]))

(def property-types-to-omit #{"Info"})

(defn- update-param-value
  "Updates the parameter value in a property definition."
  [prop param-value]
  (assoc prop :param_value {:value param-value}))

(defn- split-sequential-param-values
  "Splits a single sequential parameter value into several copies of the parameter."
  [prop param-values]
  (map (partial update-param-value prop) param-values))

(defn- normalize-list-selection
  "Normalizes the value of a list selection property."
  [param-value]
  (assoc param-value
    :display (:display param-value (:name param-value ""))
    :value   (:value param-value (:name param-value ""))))

(defn- normalize-property-value
  "Normalizes the parameter value within a property in the property value service."
  [{v :param_value :as prop}]
  (cond (sequential? v) (split-sequential-param-values prop v)
        (map? v)        [(update-param-value prop (normalize-list-selection v))]
        :else           [(update-param-value prop v)]))

(defn format-property-values-response
  "Normalizes the values in the output for the property value service."
  [output]
  (update-in output [:parameters] (partial mapcat normalize-property-value)))

(defn- implicit-output?
  [param]
  (and (= "Output" (:type param)) (:is_implicit param)))

(defn- omit-param?
  [param]
  (or (contains? property-types-to-omit (:type param))
      (implicit-output? param)))

(defn- get-full-param-id
  [param]
  (str (:step_name param) "_" (:id param)))

(defn- blank->empty-str
  [value]
  (if (string/blank? value)
    ""
    value))

(defn- format-config-value
  [config-value]
  (if (or (sequential? config-value) (map? config-value))
    config-value
    (str config-value)))

(defn- format-job-param-value
  [config config-key default-value]
  (if-let [config-value (config-key config)]
    (format-config-value config-value)
    default-value))

(defn- format-job-param
  [config param]
  (let [full-param-id (get-full-param-id param)
        config-key    (keyword full-param-id)
        default-value (blank->empty-str (:default_value param))
        value         (format-job-param-value config config-key default-value)]
    {:param_type       (:type param)
     :data_format      (blank->empty-str (:data_format param))
     :info_type        (blank->empty-str (:info_type param))
     :is_visible       (:is_visible param true)
     :param_id         (:id param)
     :full_param_id    full-param-id
     :param_value      value
     :is_default_value (= value (:default_value param))
     :param_name       (:label param)}))

(defn- prep-agave-param
  [step-id agave-app-id param]
  (let [is-file-param? (re-find #"^(?:File|Folder)" (:type param))]
    {:data_format     (when is-file-param? "Unspecified")
     :info_type       (when is-file-param? "PlainText")
     :omit_if_blank   false
     :is_visible      (:isVisible param)
     :name            (:name param)
     :is_implicit     false
     :external_app_id agave-app-id
     :ordering        (:order param)
     :type            (:type param)
     :step_id         step-id
     :label           (:label param)
     :id              (:id param)
     :description     (:description param)
     :default_value   (:defaultValue param)}))

(defn- load-agave-app-params
  [agave-client {step-id :step_id agave-app-id :external_app_id}]
  (mu/assert-agave-enabled agave-client)
  (->> (.getApp agave-client agave-app-id)
       (:groups)
       (mapcat :properties)
       (map (partial prep-agave-param step-id agave-app-id))))

(defn- load-agave-pipeline-params
  [agave-client app-id]
  (->> (ap/load-app-steps app-id)
       (remove (comp nil? :external_app_id))
       (mapcat (partial load-agave-app-params agave-client))))

(defn- load-mapped-params
  [app-id]
  (let [format-id     (partial string/join "_")
        get-source-id (comp format-id (juxt :source_name :output_id))
        get-target-id (comp format-id (juxt :target_name :input_id))
        get-ids       (juxt get-source-id get-target-id)]
    (set (mapcat get-ids (ap/load-app-mappings app-id)))))

(defn- get-job-params
  [agave-client app-id job-id config]
  (let [agave-params   (load-agave-pipeline-params agave-client app-id)
        de-params      (filter (comp nil? :external_app_id) (ap/get-app-properties app-id))
        params         (concat agave-params de-params)
        mapped-params  (load-mapped-params app-id)]
    (->> (concat agave-params de-params)
         (remove (comp mapped-params get-full-param-id))
         (remove omit-param?)
         (map (partial format-job-param config)))))

(defn format-job-params
  [agave-client app-id job-id config]
  (format-property-values-response
   {:analysis_id app-id
    :parameters (get-job-params agave-client app-id job-id config)}))
