(ns facepalm.c193-2014121701
  (:use korma.core))

(def ^:private version
  "The destination database version."
  "1.9.3:20141217.01")

(defn add-jobs-table-indexes
  []
  (println "\t* adding new indexes to the jobs table...")
  (exec-raw "CREATE INDEX jobs_parent_id_index ON jobs(parent_id)")
  (exec-raw "CREATE INDEX jobs_user_id_index ON jobs(user_id)"))

(defn convert
  "Performs the database conversion."
  []
  (println "Performing the conversion for" version)
  (add-jobs-table-indexes))
