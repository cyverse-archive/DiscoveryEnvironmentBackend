(ns facepalm.c187-2014042901
  (:use [korma.core])
  (:import [java.util UUID]))

(def ^:private version
  "The destination database version."
  "1.8.7:20140429.01")

(defn- add-sha1-column
  []
  (println "\t* adding the sha1 column to the tree_urls table")
  (exec-raw
   "ALTER TABLE tree_urls ADD COLUMN sha1 VARCHAR(40) UNIQUE NOT NULL")
  (exec-raw
   "CREATE INDEX tree_urls_sha1
    ON tree_urls(sha1)"))

(defn convert
  "Performs the conversion for database version 1.8.6:20140429.01"
  []
  (println "Performing conversion for " version)
  (add-sha1-column))
