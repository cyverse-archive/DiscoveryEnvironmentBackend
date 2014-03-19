(ns metadactyl.validation
  (:use [clojure.string :only [blank?]]
        [slingshot.slingshot :only [throw+]]))

(defn missing-json-field-exception
  "Thrown when a required field is missing from a JSON request body."
  [path]
  {:type ::request_body_missing_required_element
   :path path})

(defn expecting-json-array-exception
  "Thrown when an array is expected in a JSON request body and something else
   is found."
  [path]
  {:type ::array_expected_in_request_body_field
   :path path})

(defn expecting-json-object-exception
  "Thrown when an object is expected in a JSON request body and something else
   is found."
  [path]
  {:type ::object_expected_in_request_body_field
   :path path})

(defn expecting-string-exception
  "Thrown when a string is expected in a JSON request body and something else
   is found."
  [path]
  {:type ::string_expected_in_request_body_field
   :path path})

(defn blank-json-field-value-exception
  "Thrown when the value of a required string field is blank in a JSON request
   body"
  [path]
  {:type ::non_empty_string_required_in_request_body_field
   :path path})

(defn validate-value
  "Validates a value, presumably obtained from the HTTP request."
  [is-valid-fn value exception-fn]
  (when-not (is-valid-fn value)
    (throw+ exception-fn)))

(defn- add-field
  "Adds a named field to a path.  The key into the path may be a keyword, a
   symbol or a string."
  [path k]
  (let [key-name (cond
                  (keyword? k) (name k)
                  (symbol? k)  (name k)
                  :else        k)]
    (if (blank? path) key-name (str path "." key-name))))

(defn- add-index
  "Adds a numeric index number to a path."
  [path i]
  (str path "[" i "]"))

(defn- validate-json-field*
  "Verifies that a JSON field is present in a JSON object that has been
   translatd to a map."
  [m k path]
  (let [ex-fn #(missing-json-field-exception path)]
    (validate-value (comp not nil?) (m k) ex-fn)))

(defn validate-json-object
  "Validates a JSON object to verify that it is, in fact, an object.  The first
   argument contains a value that was pulled out of a parsed JSON object.  The
   second argument, if provided, contains the path to that JSON object.  The
   third argument, if provided, contains a function that can be used to validate
   the contents of the object."
  ([m]
     (validate-json-object m ""))
  ([m path]
     (validate-json-object m path nil))
  ([m path obj-val-fn]
     (validate-value map? m #(expecting-json-object-exception path))
     (when (fn? obj-val-fn)
       (obj-val-fn m path))))

(defn validate-json-array
  "Validates a JSON array to verify that it is, in fact, an array.  The first
   argument contains a value that was pulled out of a parsed JSON object.  The
   second argument, if provided, contains the path to the array.  The third
   argument, if provided, contains a function that can be used to validate each
   element in the array."
  ([s]
     (validate-json-array s ""))
  ([s path]
     (validate-json-array s path nil))
  ([s path elm-val-fn]
     (validate-value sequential? s #(expecting-json-array-exception path))
     (when (fn? elm-val-fn)
       (dorun (map #(elm-val-fn % (add-index path %2)) s (range))))))

(defn validate-json-object-array
  "Validates a JSON array containing JSON objects to verify that it is an array
   and that each element is a JSON object.  The first argument contains a value
   that was pulled out of a parsed JSON object.  The second argument, if
   provided, contains the path to the array.  The third argument, if provided,
   conains a function that can be used to validate the fields within each
   element of the array."
  ([s]
     (validate-json-object-array s ""))
  ([s path]
     (validate-json-object-array s path nil))
  ([s path elm-val-fn]
     (let [comp-val-fn #(validate-json-object % %2 elm-val-fn)]
       (validate-json-array s path comp-val-fn))))

(defn validate-json-array-field
  "Validates a field in a JSON object that is expected to contain a sequence.
   The first parameter contains a map representing a JSON object.  The second
   argument contains the key used to access the field being validated.  The
   third argument, if present, contains a string representing the path to the
   map in the original request JSON.  The fourth argument, if present, contains
   a function that will be used to validate each element in the sequence."
  ([m k]
     (validate-json-array-field m k ""))
  ([m k path]
     (validate-json-array-field m k path nil))
  ([m k path elm-val-fn]
     (let [path (add-field path k)]
       (validate-json-field* m k path)
       (validate-json-array (m k) path elm-val-fn))))

(defn validate-json-object-array-field
  "Validates a field in a JSON object that is expected to contain a sequence
   that, in turn, contains JSON objects.  The first parameter contains a map
   representing a JSON object.  The second argument contains the key used to
   access the field being validated.  The third argument, if present, contains a
   string representing the path to the map in the original request JSON.  The
   fourth argument, if present, contains a function that will be used to
   validate the contents of each element in the sequence."
  ([m k]
     (validate-json-object-array-field m k ""))
  ([m k path]
     (validate-json-object-array-field m k path nil))
  ([m k path elm-val-fn]
     (let [path (add-field path k)]
       (validate-json-field* m k path)
       (validate-json-object-array (m k) path elm-val-fn))))

(defn validate-json-object-field
  "Validates a field in a JSON object that is expected to contain another JSON
   object.  The first parameter contains a map representing a JSON object."
  ([m k]
     (validate-json-object-field m k ""))
  ([m k path]
     (validate-json-object-field m k path nil))
  ([m k path obj-val-fn]
     (let [path (add-field path k)]
       (validate-json-field* m k path)
       (validate-json-object (m k) path obj-val-fn))))

(defn validate-required-json-string-field
  "Verifies that a required JSON string field is present and not blank in a JSON
   object.  The given path is a string representation of the path up to the
   current JSON object."
  ([m k]
     (validate-required-json-string-field m k ""))
  ([m k path]
     (let [path (add-field path k)
           v    (m k)]
       (validate-json-field* m k path)
       (validate-value string? v #(expecting-string-exception path))
       (validate-value (comp not blank?) v
                       #(blank-json-field-value-exception path)))))

(defn validate-json-field
  "Verifies that a JSON field is present in a JSON object that has been
   translated to a map.  The given path is a string representation of the
   path up to the current JSON element."
  ([m k]
     (validate-json-field m k ""))
  ([m k path]
     (validate-json-field* m k (add-field path k))))
