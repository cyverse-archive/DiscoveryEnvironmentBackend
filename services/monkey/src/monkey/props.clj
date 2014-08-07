(ns monkey.props
  "This namespace holds all of the logic for managing configuration values.")


;; TODO implement
(defn validate
  "Validates the configuration. We don't want short-circuit evaluation in this case because
   logging all missing configuration settings is helpful."
  [props log-invalid]
  true)