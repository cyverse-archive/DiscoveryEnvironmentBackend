(ns metadactyl.translations.property-values
  (:require [clojure.tools.logging :as log]))

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
