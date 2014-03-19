(ns clojure-commons.validators
  (:use [slingshot.slingshot :only [try+ throw+]]
        [clojure-commons.error-codes]))

(defn- check-missing-keys
  [a-map required-keys]
  (let [not-valid? #(not (contains? a-map %))]
    (if (some not-valid? required-keys)
      (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
               :fields     (filter not-valid? required-keys)}))))

(defn- check-map-valid
  [a-map func-map]
  (let [not-valid? #(not ((last %1) (get a-map (first %1))))
        field-seq  (seq func-map)]
    (when (some not-valid? field-seq)
      (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
               :fields     (mapv first (filter not-valid? field-seq))}))))

(defn validate-map
  [a-map func-map]
  (check-missing-keys a-map (keys func-map))
  (check-map-valid a-map func-map))

(defn validate-field
  ([field-name field-value]
     (validate-field field-name field-value (comp not nil?)))
  ([field-name field-value valid?]
     (when-not (valid? field-value)
       (throw+ {:error_code ERR_BAD_OR_MISSING_FIELD
                :field      field-name
                :value      field-value}))))
