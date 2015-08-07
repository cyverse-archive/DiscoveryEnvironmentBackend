(ns facepalm.c210-2015080701
  (:use [korma.core]))

(def ^:private version
  "The destination database version."
  "2.1.0:20150807.01")

(defn convert
  []
  (println "Performing the conversion for" version)
  (exec-raw "ALTER TABLE attributes ALTER COLUMN required SET DEFAULT FALSE"))
