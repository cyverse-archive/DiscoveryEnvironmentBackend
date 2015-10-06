(ns facepalm.c195-2015021201
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement load-sql-file]]))

(def ^:private version
  "The destination database version."
  "1.9.5:20150212.01")

(defn- add-repeat-opt-flag-column
  []
  (println "\t* adding repeat_option_flag column to file_parameters table")
  (exec-sql-statement "ALTER TABLE ONLY file_parameters ADD COLUMN repeat_option_flag boolean DEFAULT true")
  (exec-sql-statement "DROP VIEW IF EXISTS task_param_listing")
  (load-sql-file "views/07_task_param_listing.sql"))

(defn convert
  "Performs the conversion for database version 1.9.5:20150212.01"
  []
  (println "Performing the conversion for" version)
  (add-repeat-opt-flag-column))
