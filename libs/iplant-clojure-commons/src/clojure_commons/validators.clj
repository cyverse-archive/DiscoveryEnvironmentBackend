(ns clojure-commons.validators
  (:use [slingshot.slingshot :only [try+ throw+]])
  (:require [clojure-commons.exception :as cx])
  (:import [clojure.lang IPersistentMap]))


(defn ^Boolean nonnegative-int?
  "Indicates whether or not the unparsed field value contains a nonnegative integer.

   Parameters:
     str-val - the unparsed value

   Returns:
     It returns true if str-val contains a 32-bit integer value that is zero or positive, otherwise
     it returns false."
  [^String str-val]
  (boolean
    (try+
      (<= 0 (Integer/parseInt str-val))
      (catch Object _))))


(defn- check-missing
  [handle-missing a-map required]
  (let [not-valid? #(not (contains? a-map %))]
    (when (some not-valid? required)
      (handle-missing (filter not-valid? required)))))


(defn- check-valid
  [handle-invalid kvs validators]
  (let [invalid? (fn [[k v]] (when-let [valid? (get validators k)]
                               (not (valid? v))))
        invalids (->> kvs
                   seq
                   (filter invalid?)
                   flatten
                   (apply hash-map))]
    (when-not (empty? invalids)
      (handle-invalid invalids))))


(defn- throw-missing-fields
  [fields]
  (throw+ {:type ::cx/missing-request-field, :fields fields}))


(defn- throw-bad-fields
  [fields]
  (throw+ {:type ::cx/bad-request-field, :fields (keys fields)}))


(defn validate-map
  [a-map func-map]
  (check-missing throw-missing-fields a-map (keys func-map))
  (check-valid throw-bad-fields a-map func-map))


(defn validate-field
  ([field-name field-value]
     (validate-field field-name field-value (comp not nil?)))
  ([field-name field-value valid?]
     (when-not (valid? field-value)
       (throw+ {:type  ::cx/bad-request-field
                :field field-name
                :value field-value}))))


(defn- throw-missing-params
  [params]
  (throw+ {:type   ::cx/missing-query-params
           :parameters params}))


(defn- throw-bad-params
  [params]
  (throw+ {:type ::cx/bad-query-params
           :parameters params}))


(defn validate-query-params
  "Given a set of URL query parameters and a set of corresponding validation functions, this
   function first verifies that the query parameters are present, and then validates the values.

   The validation map is a mapping of the parameter name to its validator. Each validator is a
   predicate that accepts the unparsed parameter value and returns whether or not the value is
   valid. The validation map serves a second purpose. It indicates whether or not the parameter is
   required. The presence of the parameter in the map indicates that it is required.

   Parameters:
     params     - the parameter map. It is a map of parameter names to their unparsed values. It
                  should have the following form.

                    {:<param-1> <value-1>
                     :<param-2> <value-2>
                     ...
                     :<param-n> <value-n>}

     validators - the validation map. It should have the following form.

                    {:<param-1> <validator-1>
                     :<param-2> <validator-2>
                     ...
                     :<param-n> <validator-n>}

                  Each validator should be a function of the form (^Boolean [^String]).

   Throws:
     If any of the parameters are missing, a map with the following fields is thrown.

       :error_code - ERR_MISSING_QUERY_PARAMETER
       :parameters - [a list of keys associated with the missing parameters]

     If all of the parameters are present, but some of them have bad values, a map with the
     following fields is thrown.

       :error_code - ERR_BAD_QUERY_PARAMETER
       :parameters - The params map filtered for those parameters with bad values."
  [^IPersistentMap params ^IPersistentMap validators]
  (check-missing throw-missing-params params (keys validators))
  (check-valid throw-bad-params params validators))

(defn user-owns-app?
  "Checks if the given user owns the given app, determined by comparing the user's email with the
   app's integrator_email."
  [{:keys [email]} {:keys [integrator_email]}]
  (= email integrator_email))
