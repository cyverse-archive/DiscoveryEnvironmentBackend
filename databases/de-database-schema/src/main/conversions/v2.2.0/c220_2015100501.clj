(ns facepalm.c220-2015100501
  (:use [korma.core]
        [kameleon.sql-reader :only [load-sql-file]]))

(def ^:private version
  "The destination database version."
  "2.2.0:20151005.01")

(defn- update-app-listing
  []
  (println "\t* Updating VIEW app_listing")
  (exec-raw "DROP VIEW IF EXISTS app_listing")
  (load-sql-file "views/03_app_listing.sql"))

(defn- update-tool-listing
  []
  (println "\t* Updating VIEW tool_listing")
  (exec-raw "DROP VIEW IF EXISTS tool_listing")
  (load-sql-file "views/04_tool_listing.sql"))

(defn convert
  "Performs the conversion for database version 2.2.0:20151005.01"
  []
  (println "Performing the conversion for" version)
  (update-app-listing)
  (update-tool-listing))
