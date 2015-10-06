(ns facepalm.c195-2015011401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.9.5:20150114.01")

(defn- add-not-empty-constraint
  [table-name column-name]
  (let [check-name (str table-name "_" column-name "_check")]
    (exec-raw (str "ALTER TABLE " table-name " ADD CONSTRAINT " check-name
                   " CHECK (" column-name " ~ '\\S')"))))

(defn- add-integrator-checks
  []
  (println "\t* adding empty string checks to the integration_data table")
  (dorun (map (partial add-not-empty-constraint "integration_data")
              ["integrator_name" "integrator_email"])))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-integrator-checks))
