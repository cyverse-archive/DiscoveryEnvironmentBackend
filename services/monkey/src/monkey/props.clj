(ns monkey.props
  "This namespace holds all of the logic for managing configuration values."
  (:require [clojure.set :as set]))


(def ^{:private true :const true} prop-names #{})


(defn validate
  "Validates the configuration. We don't want short-circuit evaluation in this case because
   logging all missing configuration settings is helpful.

   Parameters:
     props       - The property map to validate
     log-missing - The function used to log invalid properties. It accepts the name of the missing
                   property as its only argument.

   Returns:
     It returns true if all of the required parameters are present and false otherwise."
  [props log-missing]
  (let [missing-props (set/difference prop-names (set (keys props)))
        all-present   (empty? missing-props)]
    (when-not all-present
      (doseq [missing-prop missing-props]
        (log-missing missing-prop)))
    all-present))
