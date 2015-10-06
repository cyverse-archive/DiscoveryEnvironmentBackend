(ns facepalm.c195-2015011301
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.5:20150113.01")

(defn- implicit-input-workaround-subquery
  []
  (subselect :task_param_listing
             (fields :id)
             (where {:ordering   [< 0]
                     :value_type "Input"
                     :name       ""})))

(defn- convert-implicit-inputs
  []
  (println "\t* marking inputs with negative command-line ordering as implicit.")
  (update :file_parameters
          (set-fields {:is_implicit true})
          (where {:parameter_id [in (implicit-input-workaround-subquery)]})))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (convert-implicit-inputs))
