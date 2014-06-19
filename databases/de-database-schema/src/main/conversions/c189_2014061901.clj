(ns facepalm.c189-2014061901
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140619.01")

(defn- add-external-app-id-column
  "Adds a new column for the external app ID in the transformations table."
  []
  (println "\t* adding the external_app_id column to the transformations table")
  (exec-raw
   "ALTER TABLE transformations
    ADD COLUMN external_app_id VARCHAR(255)"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-external-app-id-column))
