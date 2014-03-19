(ns facepalm.c182-2013090301
  (:use [korma.core]
        [kameleon.core])
  (:require [clojure.string :as string]))

(def ^:private version
  "The destination database version."
  "1.8.2:20130903.01")

(defn- build-sql
  "Builds an SQL statement from a sequence of strings."
  [& strs]
  (string/join " " strs))

(defn- create-seq
  "Creates a sequence in the database."
  [seq-name]
  (exec-raw (build-sql "CREATE SEQUENCE" seq-name)))

(defn- get-max-value
  "Gets the maximum value in a table column."
  [table-name column-name]
  ((comp :max first)
   (select table-name
           (fields [(sqlfn coalesce (sqlfn max (keyword column-name)) 0) :max]))))

(defn- set-initial-seq-value
  "Sets the initial value of a sequence to the maximum value in a table column or zero if no rows
   have a value for that column."
  [table-name column-name seq-name]
  (let [max-value (get-max-value table-name column-name)]
    (select seq-name
            (fields (sqlfn setval seq-name (inc max-value) false)))))

(defn- set-default-to-seq
  "Sets the default value of a column to the next value in a sequence."
  [table-name column-name seq-name]
  (exec-raw
   (build-sql
    "ALTER TABLE" table-name
    "ALTER COLUMN" column-name
    "SET DEFAULT"
    (str "nextval('" seq-name "'::regclass)"))))

(defn- add-id-seq
  "Adds an ID sequence to a table."
  [[table-name id-column-name]]
  (println "\t* adding an ID sequence for the" id-column-name "column of" table-name)
  (let [seq-name (str table-name "_id_seq")]
    (create-seq seq-name)
    (set-initial-seq-value table-name id-column-name seq-name)
    (set-default-to-seq table-name id-column-name seq-name)))

(def ^:private tables-to-convert
  "The names of the tables to convert along with the names of their ID columns."
  [["notification_set"        "hid"]
   ["notification"            "hid"]
   ["transformation_activity" "hid"]
   ["input_output_mapping"    "hid"]
   ["template"                "hid"]
   ["property_group"          "hid"]
   ["property"                "hid"]
   ["property_type"           "hid"]
   ["value_type"              "hid"]
   ["validator"               "hid"]
   ["rule"                    "hid"]
   ["rule_type"               "hid"]
   ["rule_subtype"            "hid"]
   ["dataobjects"             "hid"]
   ["multiplicity"            "hid"]
   ["info_type"               "hid"]])

(defn convert
  "Performs the conversion for database version 1.8.2:20130903.01."
  []
  (println "Performing conversion for" version)
  (dorun (map add-id-seq tables-to-convert)))
