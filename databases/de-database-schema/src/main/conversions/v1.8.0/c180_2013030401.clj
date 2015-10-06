(ns facepalm.c180-2013030401
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "1.8.0:20130304.01")

(defn- drop-dataelementpreservation-table
  "Drops the dataelementpreservation table, which is no longer used."
  []
  (println "\t* dropping the dataelementpreservation table")
  (exec-raw "DROP TABLE dataelementpreservation CASCADE"))

(defn- drop-importedworkflow-table
  "Drops the importedworkflow table, which is no longer used."
  []
  (println "\t* dropping the importedworkflow table")
  (exec-raw "DROP TABLE importedworkflow CASCADE"))

(defn convert
  "Performs the database conversion for DE version 1.8.0:20130304.01."
  []
  (println "Performing conversion for" version)
  (drop-dataelementpreservation-table)
  (drop-importedworkflow-table))
