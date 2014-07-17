(ns donkey.services.metadata.property-values
  (:use [slingshot.slingshot :only [throw+]])
  (:require [clojure.tools.logging :as log]
            [clojure-commons.error-codes :as ce]
            [donkey.persistence.apps :as ap]
            [donkey.persistence.jobs :as jp]
            [donkey.services.metadata.agave-apps :as agave-apps]
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

(defn- format-property-values-response
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
  (if (clojure.string/blank? value)
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

(defn- format-de-job-param
  [config param]
  (let [full-param-id (get-full-param-id param)
        value (format-job-param-value config
                                      (keyword full-param-id)
                                      (blank->empty-str (:default_value param)))]
    {:param_type (:type param)
     :data_format (blank->empty-str (:data_format param))
     :info_type (blank->empty-str (:info_type param))
     :is_visible (:is_visible param true)
     :param_id (:id param)
     :full_param_id full-param-id
     :param_value value
     :is_default_value (= value (:default_value param))
     :param_name (:label param)}))

(defn- format-agave-job-param
  [config step-name param]
  (format-de-job-param config
                       (assoc param
                         :step_name step-name
                         :is_visible (:isVisible param true))))

(defn- format-agave-job-params
  [agave-client config step-name external-id]
  (let [app (.getApp agave-client external-id)
        params (mapcat :properties (:groups app))]
    (map (partial format-agave-job-param config step-name) params)))

(defn- get-agave-job-params
  [agave-client job-id config step-name external-id]
  (when-not agave-client
      (throw+ {:error_code ce/ERR_BAD_REQUEST
               :reason     "HPC_JOBS_DISABLED"}))
  (if-let [agave-params (not-empty (:parameters (agave-apps/get-agave-job-params agave-client job-id)))]
    agave-params
    (format-agave-job-params agave-client config step-name external-id)))

(defn- format-job-param
  [agave-client job-id config results param]
  (if-let [external-id (:external_app_id param)]
    (concat results (get-agave-job-params agave-client job-id config (:step_name param) external-id))
    (conj results (format-de-job-param config param))))

(defn format-job-params
  [agave-client app-id job-id config]
  (let [params (remove omit-param? (ap/get-app-properties app-id))
        params (reduce (partial format-job-param agave-client job-id config) [] params)]
    (format-property-values-response
     {:analysis_id app-id
      :parameters params})))

