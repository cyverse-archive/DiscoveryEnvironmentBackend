(ns donkey.services.metadata.property-values
  (:require [clojure.tools.logging :as log]
            [donkey.persistence.apps :as ap]
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

(defn- format-job-param-value
  [config param]
  (if-let [config-value (get config (keyword (get-full-param-id param)))]
    (if (or (sequential? config-value) (map? config-value))
      config-value
      (str config-value))
    (blank->empty-str (:default_value param))))

(defn- format-job-param
  [config param]
  (let [param_id (:id param)
        value (format-job-param-value config param)]
    {:param_type (:type param)
     :data_format (blank->empty-str (:data_format param))
     :info_type (blank->empty-str (:info_type param))
     :is_visible (:is_visible param true)
     :param_id param_id
     :full_param_id (get-full-param-id param)
     :param_value value
     :is_default_value (= value (:default_value param))
     :param_name (:label param)}))

(defn format-job-params
  [job]
  (let [app-id (:app_id job)
        params (remove omit-param? (ap/get-app-properties app-id))
        config (:config (service/decode-json (.getValue (:submission job))))]
    (format-property-values-response
     {:analysis_id app-id
      :parameters (map (partial format-job-param config) params)})))

