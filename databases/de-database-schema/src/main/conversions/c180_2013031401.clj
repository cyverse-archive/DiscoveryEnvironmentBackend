(ns facepalm.c180-2013031401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130314.01")

(defn- fix-version-length
  "Updates the length of the version column in the tool_requests table to match the length of the
   same column in the deployed_components table."
  []
  (println "\t* fixing the length of the Version column in the tool_requests table.")
  (exec-raw
   "ALTER TABLE tool_requests
    ALTER COLUMN version TYPE varchar(255)"))

(defn convert
  "Performs the conversion for database version 1.8.0:20130314.01."
  []
  (println "Performing conversion for" version)
  (fix-version-length))
