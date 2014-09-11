(ns clojure-commons.config
  (:use [clojure.java.io :only [file]]
        [slingshot.slingshot :only [throw+]])
  (:require [clojure.string :as string]
            [clojure-commons.error-codes :as ce]
            [clojure-commons.props :as cp]
            [clojure.tools.logging :as log])
  (:import [java.io IOException]))

(defn load-config-from-file
  "A multi-arity function that loads the configuration properties from a file.

   Parameters:
       conf-dir - the path to the configuration directory.
       filename - the name of the configuration file.
       props    - the reference to the properties.

   or:
       filepath - the path to the configuration file.
       props    - the reference to the properties."
  ([filepath props]
   (dosync (ref-set props (cp/read-properties (file filepath)))))

  ([conf-dir filename props]
   (if (nil? conf-dir)
     (dosync (ref-set props (cp/read-properties (file filename))))
     (dosync (ref-set props (cp/read-properties (file conf-dir filename)))))))

(defn masked-field?
  "Returns a truthy value if the field should be masked and a falsey value if it shouldn't."
  [field filters]
  (some #(re-find % field) filters))

(defn- log-prop
  "Logs a single configuration setting."
  [[k v] filters]
  (let [v (if (masked-field? k filters) "********" v)]
    (log/info "CONFIG:" k "=" v)))

(defn- mask-prop
  "Masks a single configuration setting, if necessary. Does not log it."
  [[k v] filters]
  [k (if (masked-field? k filters) "********" v)])

(defn log-config
  "Logs the configuration settings.

   Parameters:
       props - the reference to the properties.
       :filters - list of regexes matching keys in a properties file
                  whose values should be masked. Defaults to: [#\"passord\" #\"password\"].
                  The defaults will be added to the :filters list anyway, so you don't need
                  to specify them."
  [props & {:keys [filters]
            :or {filters []}}]
  (let [all-filters (concat filters [#"password" #"pass"])
        log-it      #(log-prop % all-filters)]
    (dorun (map log-it (sort-by key @props)))))

(defn mask-config
  "Returns a new configuration map with the appropriate fields masked.

   Parameters:
       props - the reference to the properties map.
       :filters - same as what's passed in to (log-config)"
  [props & {:keys [filters]
            :or {filters []}}]
  (let [all-filters (concat filters [#"password" #"pass"])
        mask-it     #(mask-prop % all-filters)]
    (into {} (mapv mask-it (sort-by key @props)))))

(defn record-missing-prop
  "Records a property that is missing.  Instead of failing on the first missing parameter, we log
   the missing parameter, mark the configuration as invalid and keep going so that we can log as
   many configuration errors as possible in one run.

   Parameters:
       prop-name    - the name of the property.
       config-valid - a ref containing a validity flag."
  [prop-name config-valid]
  (log/error "required configuration setting" prop-name "is empty or"
             "undefined")
  (dosync (ref-set config-valid false)))

(defn record-invalid-prop
  "Records a property that has an invalid value.  Instead of failing on the first missing
   parameter, we log the missing parameter, mark the configuration as invalid and keep going so
   that we can log as many configuration errors as possible in one run.

   Parameters:
       prop-name    - the name of the property.
       t            - the Throwable instance that caused the error.
       confiv-valid - a ref containing a validity flag."
  [prop-name t config-valid]
  (log/error "invalid configuration setting for" prop-name ":" t)
  (dosync (ref-set config-valid false)))

(defn get-required-prop
  "Gets a required property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing a validity flag."
  [props prop-name config-valid]
  (let [value (get @props prop-name "")]
    (when (empty? value)
      (record-missing-prop prop-name config-valid))
    value))

(defn get-optional-prop
  "Gets an optional property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing a validity flag.
       default      - the default property value."
  ([props prop-name config-valid]
     (get @props prop-name ""))
  ([props prop-name config-valid default]
     (get @props prop-name default)))

(defn vector-from-prop
  "Derives a list of values from a single comma-delimited value.

   Parameters:
       value - the value to convert to a vector."
  [value]
  (remove string/blank? (string/split value #", *")))

(defn get-required-vector-prop
  "Gets a required vector property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing a validity flag."
  [props prop-name config-valid]
  (vector-from-prop (get-required-prop props prop-name config-valid)))

(defn get-optional-vector-prop
  "Gets an optional vector property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing a validity flag."
  ([props prop-name config-valid]
     (vector-from-prop (get-optional-prop props prop-name config-valid)))
  ([props prop-name config-valid default]
     (if-let [string-value (get-optional-prop props prop-name config-valid nil)]
       (vector-from-prop string-value)
       default)))

(defn string-to-int
  "Attempts to convert a String property to an integer property.  Returns zero if the property
   can't be converted.

   Parameters:
       prop-name    - the name of the property.
       value        - the value of the property as a string.
       config-valid - a ref containing a vailidity flag."
  [prop-name value config-valid]
  (try
    (Integer/parseInt value)
    (catch NumberFormatException e
      (record-invalid-prop prop-name e config-valid)
      0)))

(defn string-to-long
  "Attempts to convert a String property to a long property.  Returns zero if the property can't
   be converted.

   Parameters:
       prop-name    - the name of the property.
       value        - the value of the property as a string.
       config-valid - a ref containing a validity flag."
  [prop-name value config-valid]
  (try
    (Long/parseLong value)
    (catch NumberFormatException e
      (record-invalid-prop prop-name e config-valid)
      0)))

(defn string-to-boolean
  "Attempts to convert a String property to a Boolean property.  Returns false if the property
   can't be converted.

   Parameters:
       props        - a ref containing the properties.
       value        - the value of the property as a string.
       config-valid - a ref containing a validity flag."
  [prop-name value config-valid]
  (try
    (Boolean/parseBoolean value)
    (catch Exception e
      (record-invalid-prop prop-name e config-valid)
      false)))

(defn get-required-integer-prop
  "Gets a required integer property from a set of properties.  If the property is missing or not
   able to be converted to an integer then the configuration will be marked as invalid and zero
   will be returned.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing a validity flag."
  [props prop-name config-valid]
  (let [value (get-required-prop props prop-name config-valid)]
    (if (string/blank? value)
      0
      (string-to-int prop-name value config-valid))))

(defn get-optional-integer-prop
  "Gets an optional integer property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing the validity flag.
       default      - the default value."
  ([props prop-name config-valid]
     (get-optional-integer-prop props prop-name config-valid 0))
  ([props prop-name config-valid default]
     (if-let [string-value (get-optional-prop props prop-name config-valid nil)]
       (string-to-int prop-name string-value config-valid)
       default)))

(defn get-required-long-prop
  "Gets a required long property from a set of properties.  If a property is missing or not able to
   be converted to a long then the configuration will be marked as invalid and zero will be
   returned.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of a property.
       config-valid - a ref containing a validity flag."
  [props prop-name config-valid]
  (let [value (get-required-prop props prop-name config-valid)]
    (if (string/blank? value)
      0
      (string-to-long prop-name value config-valid))))

(defn get-optional-long-prop
  "Gets an optional long property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing the vailidity flag.
       default      - the default value."
  ([props prop-name config-valid]
     (get-optional-long-prop props prop-name config-valid 0))
  ([props prop-name config-valid default]
     (if-let [string-value (get-optional-prop props prop-name config-valid nil)]
       (string-to-long prop-name string-value config-valid)
       default)))

(defn get-required-boolean-prop
  "Gets a required Boolean property from a set of properties.  If a property is missing or not able
   to be converted to a Boolean then the configuration will be marked as invalid and false will
   be returned.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of a property.
       config-valid - a ref containing a validity flag."
  [props prop-name config-valid]
  (let [value (get-required-prop props prop-name config-valid)]
    (if (string/blank? value)
      false
      (string-to-boolean prop-name value config-valid))))

(defn get-optional-boolean-prop
  "Gets an optional Boolean property from a set of properties.

   Parameters:
       props        - a ref containing the properties.
       prop-name    - the name of the property.
       config-valid - a ref containing the vailidity flag.
       default      - the default value."
  ([props prop-name config-valid]
     (get-optional-boolean-prop props prop-name config-valid false))
  ([props prop-name config-valid default]
     (if-let [string-value (get-optional-prop props prop-name config-valid nil)]
       (string-to-boolean prop-name string-value config-valid)
       default)))

(defn- wrap-extraction-fn
  "Places a property extraction function in an appropriate wrapper, depending on whether or not
   the property is flagged. This function depends on the fact that property validation is done
   in the extraction function itself. For flagged properties that are disabled, the extraction
   function is not called when the value is retrieved. Therefore the validation for disabled
   properties is skipped because the extraction function isn't called.

   Parameters:
       extraction-fn - the actual extraction function.
       flag-props    - the feature flag properties determining if the property is relevant."
  [extraction-fn flag-props]
  (if (empty? flag-props)
    `(memoize ~extraction-fn)
    `(memoize (fn [] (when (some #(%) ~(vec flag-props)) (~extraction-fn))))))

(defn define-property
  "Defines a property. This is a helper function that performs common tasks required by all of the
   defprop macros.

   Parameters:
       sym           - the symbol to define.
       desc          - a brief description of the property.
       configs       - a ref containing the list of config settings.
       flag-props    - the feature flag properties determining if the property is relevant.
       extraction-fn - the function used to extract the property value."
  [sym desc configs flag-props extraction-fn]
  (let [wrapped-extraction-fn (wrap-extraction-fn extraction-fn flag-props)]
    `(dosync (alter ~configs conj (def ~sym ~desc ~wrapped-extraction-fn)))))

(defn define-required-property
  "Defines a required property. This is a helper function that performs common tasks required by
   the macros for required properties.

   Parameters:
       sym           - the symbol to define.
       desc          - a brief description of the property.
       props         - a ref containing the properties.
       config-valid  - a ref containing the validity flag.
       configs       - a ref containing the list of config settings.
       flag-props    - the feature flag properties determining if the property is relevant.
       prop-name     - the name of the property.
       extraction-fn - the function used to extract the property value."
  [sym desc [props config-valid configs flag-props] prop-name extraction-fn]
  (define-property sym desc configs flag-props
    `(fn [] (~extraction-fn ~props ~prop-name ~config-valid))))

(defn define-optional-property
  "Defines an optional property. This is a helper function that performs common tasks required by
   the macros for optional properties.

   Parameters:
       sym           - the symbol to define.
       desc          - a brief description of the property.
       props         - a ref containing the properties.
       config-valid  - a ref containing the validity flag.
       configs       - a ref containing the list of config settings.
       flag-props    - the feature flag properties determining if the property is relevant.
       prop-name     - the name of the property.
       extraction-fn - the function used to extract the property value.
       default-value - the default value for the property."
  [sym desc [props config-valid configs flag-props] prop-name extraction-fn default-value]
  (define-property sym desc configs flag-props
    `(fn [] (~extraction-fn ~props ~prop-name ~config-valid ~default-value))))

(defmacro defprop-str
  "defines a required string property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property."
  [sym desc [props config-valid configs & flag-props] prop-name]
  (define-required-property
    sym desc [props config-valid configs flag-props] prop-name get-required-prop))

(defmacro defprop-optstr
  "Defines an optional string property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property.
       default      - the default value."
  ([sym desc [props config-valid configs & flag-props] prop-name]
     (define-optional-property
       sym desc [props config-valid configs flag-props] prop-name get-optional-prop ""))
  ([sym desc [props config-valid configs & flag-props] prop-name default]
     (define-optional-property
       sym desc [props config-valid configs flag-props] prop-name get-optional-prop default)))

(defmacro defprop-vec
  "Defines a required vector property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property."
  [sym desc [props config-valid configs & flag-props] prop-name]
  (define-required-property
    sym desc [props config-valid configs flag-props] prop-name get-required-vector-prop))

(defmacro defprop-optvec
  "Defines an optional vector property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property.
       default      - the default value."
  ([sym desc [props config-valid configs & flag-props] prop-name]
     (define-optional-property
       sym desc [props config-valid configs flag-props]
       prop-name get-optional-vector-prop []))
  ([sym desc [props config-valid configs & flag-props] prop-name default]
     (define-optional-property
       sym desc [props config-valid configs flag-props]
       prop-name get-optional-vector-prop default)))

(defmacro defprop-int
  "Defines a required integer property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property."
  [sym desc [props config-valid configs & flag-props] prop-name]
  (define-required-property
    sym desc [props config-valid configs flag-props] prop-name get-required-integer-prop))

(defmacro defprop-optint
  "Defines an optional integer property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing the validity flag.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property.
       default      - the default value."
  ([sym desc [props config-valid configs & flag-props] prop-name]
     (define-optional-property
       sym desc [props config-valid configs flag-props]
       prop-name get-optional-integer-prop 0))
  ([sym desc [props config-valid configs & flag-props] prop-name default]
     (define-optional-property
       sym desc [props config-valid configs flag-props]
       prop-name get-optional-integer-prop default)))

(defmacro defprop-long
  "Defines a required long property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property."
  [sym desc [props config-valid configs & flag-props] prop-name]
  (define-required-property
    sym desc [props config-valid configs flag-props] prop-name get-required-long-prop))

(defmacro defprop-optlong
  "Defined an optional long property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing the validity flag.
       prop-name    - the name of the property.
       flag-props   - the feature flag properties determining if the property is relevant.
       default      - the default value."
  ([sym desc [props config-valid configs & flag-props] prop-name]
     (define-optional-property
       sym desc [props config-valid configs flag-props] prop-name get-optional-long-prop 0))
  ([sym desc [props config-valid configs & flag-props] prop-name default]
     (define-optional-property
       sym desc [props config-valid configs flag-props] prop-name get-optional-long-prop default)))

(defmacro defprop-boolean
  "Defines a required Boolean property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing a validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property."
  [sym desc [props config-valid configs & flag-props] prop-name]
  (define-required-property
    sym desc [props config-valid configs flag-props] prop-name get-required-boolean-prop))

(defmacro defprop-optboolean
  "Defines an optional Boolean property.

   Parameters:
       sym          - the symbol to define.
       desc         - a brief description of the property.
       props        - a ref containing the properties.
       config-valid - a ref containing the validity flag.
       configs      - a ref containing the list of config settings.
       flag-props   - the feature flag properties determining if the property is relevant.
       prop-name    - the name of the property.
       default      - the default value."
  ([sym desc [props config-valid configs & flag-props] prop-name]
     (define-optional-property
       sym desc [props config-valid configs flag-props]
       prop-name get-optional-boolean-prop false))
  ([sym desc [props config-valid configs & flag-props] prop-name default]
     (define-optional-property
       sym desc [props config-valid configs flag-props]
       prop-name get-optional-boolean-prop default)))

(defn validate-config
  "Validates a configuration that has been defined and loaded using this library.

   Parameters:
       configs      - a ref containing an array of symbols that were defined.
       config-valid - a ref to the configuration validity flag."
  [configs config-valid]
  (dorun (map #(%) @configs))
  @config-valid)
