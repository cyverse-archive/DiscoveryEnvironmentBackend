(ns facepalm.c189-2014070701
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140707.01")

(defn- convert-avus-table-user-cols
  []
  (println "\t* updating user columns in the avus table")
  (exec-raw "ALTER TABLE avus DROP CONSTRAINT avus_unique")
  (exec-raw "ALTER TABLE avus ADD CONSTRAINT avus_unique UNIQUE (target_id, target_type, attribute, value, unit)")
  (exec-raw "DROP INDEX avus_owner_id_idx")
  (exec-raw "ALTER TABLE avus RENAME COLUMN owner_id TO created_by")
  (exec-raw "ALTER TABLE avus ADD COLUMN modified_by varchar(512)")
  (update :avus (set-fields {:modified_by :created_by})))

(defn convert
  "Performs the conversion for database version 1.8.9:20140707.01"
  []
  (println "Performing the conversion for" version)
  (convert-avus-table-user-cols))
