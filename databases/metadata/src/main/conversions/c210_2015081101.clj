(ns facepalm.c210-2015081101
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150811.01")

(defn- print-warning
  [& lines]
  (println)
  (dorun (map (partial println "\t ") lines)))

(defn- print-conversion-warning
  []
  (print-warning
   "WARNING: this conversion requires data to be copied from the DE database to the"
   "metadata database. Please ensure that the conversion utility that copies this data"
   "is executed after this conversion is done."))

(defn convert
  []
  (println "Performing the conversion for" version)
  (exec-raw "ALTER TABLE templates
             ALTER COLUMN created_by SET DATA TYPE varchar(512),
             ALTER COLUMN modified_by SET DATA TYPE varchar(512)")
  (exec-raw "ALTER TABLE attributes
             ALTER COLUMN created_by SET DATA TYPE varchar(512),
             ALTER COLUMN modified_by SET DATA TYPE varchar(512)")
  (print-conversion-warning))
