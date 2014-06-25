(ns facepalm.c189-2014062501
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.9:20140625.01")

(defn- create-version-table
  []
  (println "\t* creating the version table")
  (exec-raw
   "CREATE TABLE version (
    version character varying(20) NOT NULL,
    applied timestamp DEFAULT now(),
    PRIMARY KEY (version))"))

(defn convert
  "Performs the conversion for database version 1.8.9:20140625.01"
  []
  (println "Performing the conversion for" version)
  (create-version-table))
