(ns facepalm.c200-2014042401
  (:use [clojure.java.io :only [file reader]]
        [kameleon.sql-reader :only [sql-statements]]
        [korma.core])
  (:require [clojure.tools.logging :as log])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "2.0.0:20140424.01")

(defn exec-sql-statement
  "A wrapper around korma.core/exec-raw that logs the statement that is being
   executed if debugging is enabled."
  [statement]
  (log/debug "executing SQL statement:" statement)
  (exec-raw statement))

(defn- load-sql-file
  "Loads a single SQL file into the database."
  [sql-file]
  (println (str "Loading " (.getName sql-file) "..."))
  (with-open [rdr (reader sql-file)]
    (dorun (map exec-sql-statement (sql-statements rdr)))))

(defn- add-renamed-tables
  "Adds the renamed tables."
  [unpacked-dir]
  (println "\t* adding the app_categories table")
  (load-sql-file (file unpacked-dir "tables/01_app_categories.sql"))
  (println "\t* adding the tools table")
  (load-sql-file (file unpacked-dir "tables/03_tools.sql"))
  (println "\t* adding the tasks table")
  (load-sql-file (file unpacked-dir "tables/04_tasks.sql"))
  (println "\t* adding the apps table")
  (load-sql-file (file unpacked-dir "tables/05_apps.sql"))
  (println "\t* adding the app_steps table")
  (load-sql-file (file unpacked-dir "tables/07_app_steps.sql"))
  (println "\t* adding the app_category_app table")
  (load-sql-file (file unpacked-dir "tables/11_app_category_app.sql"))
  (println "\t* adding the workflow_io_maps table")
  (load-sql-file (file unpacked-dir "tables/13_workflow_io_maps.sql"))
  (println "\t* adding the file_parameters table")
  (load-sql-file (file unpacked-dir "tables/14_file_parameters.sql"))
  (println "\t* adding the tool_test_data_files table")
  (load-sql-file (file unpacked-dir "tables/15_tool_test_data_files.sql"))
  (println "\t* adding the parameters table")
  (load-sql-file (file unpacked-dir "tables/24_parameters.sql"))
  (println "\t* adding the parameter_groups table")
  (load-sql-file (file unpacked-dir "tables/25_parameter_groups.sql"))
  (println "\t* adding the parameter_values table")
  (load-sql-file (file unpacked-dir "tables/25_parameter_values.sql"))
  (println "\t* adding the parameter_types table")
  (load-sql-file (file unpacked-dir "tables/27_parameter_types.sql"))
  (println "\t* adding the validation_rules table")
  (load-sql-file (file unpacked-dir "tables/29_validation_rules.sql"))
  (println "\t* adding the validation_rule_arguments table")
  (load-sql-file (file unpacked-dir "tables/30_validation_rule_arguments.sql"))
  (println "\t* adding the app_category_group table")
  (load-sql-file (file unpacked-dir "tables/35_app_category_group.sql"))
  (println "\t* adding the app_references table")
  (load-sql-file (file unpacked-dir "tables/40_app_references.sql"))
  (println "\t* adding the tool_type_parameter_type table")
  (load-sql-file (file unpacked-dir "tables/50_tool_type_parameter_type.sql")))

(defn convert
  "Performs the database conversion."
  [unpacked-dir]
  (println "Performing the conversion for" version)
  (add-renamed-tables unpacked-dir))
