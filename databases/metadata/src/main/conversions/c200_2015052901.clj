(ns facepalm.c200-2015052901
  (:use [korma.core]
        [kameleon.sql-reader :only [exec-sql-statement]]))

(def ^:private version
  "The destination database version."
  "2.0.0:20150529.01")

(defn- delete-metadata-complete-template-attr
  []
  (println "\t* deleting 'Metadata complete' AVUs")
  (delete :avus (where {:attribute "Metadata complete"})))

(defn convert
  "Performs the conversion for this version of the database"
  []
  (println "Performing the conversion for" version)
  (delete-metadata-complete-template-attr))
