(ns facepalm.c195-2015020201
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "1.9.5:20150202.01")

(defn- add-app-documentation-table
  []
  (println "\t* adding app_documentation table")
  (load-sql-file "tables/10_app_documentation.sql")
  (load-sql-file "constraints/10_app_documentation.sql"))

(defn convert
  "Performs the conversion for database version 1.9.5:20150202.01"
  []
  (println "Performing the conversion for" version)
  (add-app-documentation-table))
