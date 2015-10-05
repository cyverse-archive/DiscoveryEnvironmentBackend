(ns facepalm.c184-2013111401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.4:20131114.01")

(defn- add-output-type-name-column
  "Adds the output_type_name column to the multiplicity table."
  []
  (println "\t* adding the output_type_name column to the multiplicity table")
  (exec-raw
   "ALTER TABLE multiplicity
    ADD COLUMN output_type_name character varying(64)"))

(defn- set-output-type
  [[multiplicity-name output-type-name]]
  (update :multiplicity
          (set-fields {:output_type_name output-type-name})
          (where {:name multiplicity-name})))

(def ^:private output-type-names
  [["many"       "MultiFileOutput"]
   ["single"     "FileOutput"]
   ["collection" "FolderOutput"]])

(defn- populate-output-type-name-column
  "Populates the output_type_name column in the multiplicity table."
  []
  (println "\t* populating the output_type_name column in the multiplicity table")
  (dorun (map set-output-type output-type-names)))

(defn convert
  "Performs the conversion for database version 1.8.4:20131114.01."
  []
  (println "Performing conversion for" version)
  (add-output-type-name-column)
  (populate-output-type-name-column))
