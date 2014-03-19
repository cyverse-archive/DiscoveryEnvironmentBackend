(ns metadactyl.translations.property-values)

(defn- normalize-property-value
  "Normalizes the parameter value within a property in the property value service."
  [prop]
  (if (and (sequential? (:param_value prop)))
    (map #(assoc prop :param_value {:value %}) (:param_value prop))
    [(update-in prop [:param_value] (fn [v] {:value v}))]))

(defn format-property-values-response
  "Normalizes the values in the output for the property value service."
  [output]
  (update-in output [:parameters] (partial mapcat normalize-property-value)))
