(ns facepalm.c195-2015021601
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "1.9.5:20150216.01")

(defn- convert-inputs-omit-if-blank-flag
  "CORE-6527: The v1.9.2 job submission endpoint used to behave as if this flag was always 'true'
   for input parameter types. Some Apps were made public with this flag set to 'false' for their
   input parameters, but depended on the behavior of the endpoint treating this flag as 'true'.
   This conversion explicitly sets the omit_if_blank flag to 'true' for input parameters created
   before v1.9.3."
  []
  (println "\t* converting omit_if_blank column in parameters table for input types")
  (exec-sql-statement
    "UPDATE parameters SET omit_if_blank = TRUE"
    "WHERE id IN ("
      "SELECT DISTINCT(t.id) FROM app_listing a"
      "JOIN app_steps step ON step.app_id = a.id"
      "JOIN task_param_listing t ON t.task_id = step.task_id"
      "WHERE omit_if_blank = FALSE"
      "AND parameter_type IN ('FileInput','FolderInput','MultiFileSelector')"
      "AND (a.integration_date < (SELECT applied FROM version WHERE version = '1.9.3:20140424.01')"
             "OR a.edited_date < (SELECT applied FROM version WHERE version = '1.9.3:20140424.01'))"
    ");"))

(defn convert
  "Performs the conversion for database version 1.9.5:20150216.01"
  []
  (println "Performing the conversion for" version)
  (convert-inputs-omit-if-blank-flag))
